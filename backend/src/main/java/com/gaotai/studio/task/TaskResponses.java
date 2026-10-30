package com.gaotai.studio.task;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public final class TaskResponses {

    private TaskResponses() {
    }

    public static TaskResponse from(TaskRecord task) {
        return new TaskResponse(
                task.getId(),
                task.getTemplateId(),
                task.getTemplateTitle(),
                task.getStatus(),
                task.getStage(),
                task.getFailureStage(),
                task.getFailureReason(),
                task.getResultUrl(),
                iso(task.getCreatedAt()),
                iso(task.getUpdatedAt()),
                iso(task.getFinishedAt()));
    }

    private static String iso(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.atOffset(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS).toString();
    }
}
