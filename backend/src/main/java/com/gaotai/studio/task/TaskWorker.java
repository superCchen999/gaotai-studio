package com.gaotai.studio.task;

import com.gaotai.studio.config.StudioProperties;
import com.gaotai.studio.config.TimeSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "studio.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TaskWorker {

    private static final Logger log = LoggerFactory.getLogger(TaskWorker.class);

    private final TaskQueue taskQueue;
    private final TaskProcessor taskProcessor;
    private final TaskMapper taskMapper;
    private final TimeSource timeSource;
    private final long idleWaitMs;
    private final Object wake = new Object();
    private volatile boolean running;
    private Thread thread;

    public TaskWorker(
            TaskQueue taskQueue,
            TaskProcessor taskProcessor,
            TaskMapper taskMapper,
            TimeSource timeSource,
            StudioProperties properties) {
        this.taskQueue = taskQueue;
        this.taskProcessor = taskProcessor;
        this.taskMapper = taskMapper;
        this.timeSource = timeSource;
        this.idleWaitMs = Math.max(50, properties.getWorker().getIdleWaitMs());
    }

    @jakarta.annotation.PostConstruct
    public void start() {
        running = true;
        thread = new Thread(this::loop, "task-worker");
        thread.setDaemon(true);
        thread.start();
    }

    @jakarta.annotation.PreDestroy
    public void stop() {
        running = false;
        nudge();
        if (thread != null) {
            thread.interrupt();
        }
    }

    public void nudge() {
        synchronized (wake) {
            wake.notifyAll();
        }
    }

    private void loop() {
        while (running) {
            String taskId = null;
            try {
                taskId = taskQueue.claimNext();
                if (taskId == null) {
                    synchronized (wake) {
                        if (running) {
                            wake.wait(idleWaitMs);
                        }
                    }
                    continue;
                }
                log.info("已领取任务 {}", taskId);
                taskProcessor.process(taskId);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (SleepInterruptedException exception) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException exception) {
                if (Thread.currentThread().isInterrupted() || !running) {
                    break;
                }
                log.error("任务执行出现未捕获异常", exception);
                if (taskId != null) {
                    markUnexpectedFailure(taskId);
                }
            }
        }
    }

    private void markUnexpectedFailure(String taskId) {
        try {
            TaskRecord current = taskMapper.findById(taskId);
            String stage = current != null && current.getStage() != null
                    ? current.getStage()
                    : TaskStage.ASSET_CHECK.name();
            taskMapper.fail(taskId, stage, "步骤执行异常", timeSource.now());
        } catch (RuntimeException exception) {
            log.error("无法记录任务 {} 的异常失败", taskId, exception);
        }
    }
}
