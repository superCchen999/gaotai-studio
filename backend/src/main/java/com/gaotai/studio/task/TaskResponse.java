package com.gaotai.studio.task;

public record TaskResponse(
        String id,
        String templateId,
        String templateTitle,
        String status,
        String stage,
        String failureStage,
        String failureReason,
        String resultUrl,
        String createdAt,
        String updatedAt,
        String finishedAt) {
}
