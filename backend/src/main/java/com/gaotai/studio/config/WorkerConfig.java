package com.gaotai.studio.config;

import com.gaotai.studio.task.TaskWakeup;
import com.gaotai.studio.task.TaskWorker;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkerConfig {

    @Bean
    public TaskWakeup taskWakeup(ObjectProvider<TaskWorker> workers) {
        return () -> workers.ifAvailable(TaskWorker::nudge);
    }
}
