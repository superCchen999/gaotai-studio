package com.gaotai.studio.task;

import com.gaotai.studio.asset.Asset;
import com.gaotai.studio.asset.AssetMapper;
import com.gaotai.studio.asset.UploadStore;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.template.Template;
import com.gaotai.studio.template.TemplateMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TaskProcessor {

    private static final Logger log = LoggerFactory.getLogger(TaskProcessor.class);

    private final TaskMapper taskMapper;
    private final AssetMapper assetMapper;
    private final TemplateMapper templateMapper;
    private final UploadStore uploadStore;
    private final ResultWriter resultWriter;
    private final SimulatorSettings simulatorSettings;
    private final Sleeper sleeper;
    private final TimeSource timeSource;

    public TaskProcessor(
            TaskMapper taskMapper,
            AssetMapper assetMapper,
            TemplateMapper templateMapper,
            UploadStore uploadStore,
            ResultWriter resultWriter,
            SimulatorSettings simulatorSettings,
            Sleeper sleeper,
            TimeSource timeSource) {
        this.taskMapper = taskMapper;
        this.assetMapper = assetMapper;
        this.templateMapper = templateMapper;
        this.uploadStore = uploadStore;
        this.resultWriter = resultWriter;
        this.simulatorSettings = simulatorSettings;
        this.sleeper = sleeper;
        this.timeSource = timeSource;
    }

    public void process(String taskId) {
        TaskRecord task = taskMapper.findById(taskId);
        if (task == null || !TaskStatus.PROCESSING.name().equals(task.getStatus())) {
            return;
        }
        String resultUrl = null;
        for (TaskStage stage : TaskStage.ordered()) {
            if (taskMapper.markStage(taskId, stage.name(), timeSource.now()) == 0) {
                return;
            }
            log.info("任务 {} 进入阶段 {}", taskId, stage);
            try {
                sleeper.sleep(simulatorSettings.get().stepDelayMs());
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }
                TaskRecord current = taskMapper.findById(taskId);
                if (current == null || !TaskStatus.PROCESSING.name().equals(current.getStatus())) {
                    return;
                }
                if (simulatorSettings.get().failStage() == SimulatedFailure.valueOf(stage.name())) {
                    fail(taskId, stage, simulatedReason(stage));
                    return;
                }
                if (stage == TaskStage.ASSET_CHECK) {
                    checkAssets(current);
                } else if (stage == TaskStage.RESULT_PACKAGING) {
                    String title = current.getTemplateTitle() == null ? "制作结果" : current.getTemplateTitle();
                    resultUrl = resultWriter.write(taskId, title);
                }
            } catch (SleepInterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (StageFailure exception) {
                fail(taskId, stage, exception.getMessage());
                return;
            } catch (RuntimeException exception) {
                log.error("任务 {} 在阶段 {} 出现异常", taskId, stage, exception);
                fail(taskId, stage, "步骤执行异常");
                return;
            }
        }
        if (resultUrl == null) {
            fail(taskId, TaskStage.RESULT_PACKAGING, "结果整理失败：未生成结果");
            return;
        }
        int updated = taskMapper.succeed(taskId, TaskStage.RESULT_PACKAGING.name(), resultUrl, timeSource.now());
        if (updated == 1) {
            log.info("任务 {} 已完成", taskId);
        }
    }

    private void checkAssets(TaskRecord task) {
        Template template = templateMapper.findById(task.getTemplateId());
        if (template == null) {
            throw new StageFailure("素材检查未通过：模板不存在");
        }
        List<String> assetIds = taskMapper.findAssetIds(task.getId());
        if (assetIds.size() != template.getRequiredImageCount()) {
            throw new StageFailure("素材检查未通过：图片数量与模板要求不一致");
        }
        List<String> allowed = template.acceptedMimeTypeList();
        for (String assetId : assetIds) {
            Asset asset = assetMapper.findById(assetId);
            if (asset == null || !task.getUserId().equals(asset.getUserId())) {
                throw new StageFailure("素材检查未通过：素材不存在或不属于当前用户");
            }
            if (!allowed.contains(asset.getMimeType())) {
                throw new StageFailure("素材检查未通过：素材类型不符合模板要求");
            }
            if (asset.getSizeBytes() > template.getMaxFileSizeBytes()) {
                throw new StageFailure("素材检查未通过：素材超过大小限制");
            }
            if (!uploadStore.exists(asset.getStorageName())) {
                throw new StageFailure("素材检查未通过：素材文件不存在或不可读");
            }
        }
    }

    private void fail(String taskId, TaskStage stage, String reason) {
        String message = reason == null || reason.isBlank() ? "步骤执行失败" : reason;
        if (message.length() > 500) {
            message = message.substring(0, 500);
        }
        int updated = taskMapper.fail(taskId, stage.name(), message, timeSource.now());
        if (updated == 1) {
            log.info("任务 {} 在阶段 {} 失败：{}", taskId, stage, message);
        }
    }

    static String simulatedReason(TaskStage stage) {
        return switch (stage) {
            case ASSET_CHECK -> "素材检查未通过：已按模拟配置在此阶段失败";
            case SIMULATE_GENERATE -> "模拟生成失败：已按模拟配置在此阶段失败";
            case RESULT_PACKAGING -> "结果整理失败：已按模拟配置在此阶段失败";
        };
    }
}
