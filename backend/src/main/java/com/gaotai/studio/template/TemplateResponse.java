package com.gaotai.studio.template;

import java.util.List;

public record TemplateResponse(
        String id,
        String title,
        String category,
        String coverUrl,
        String description,
        int requiredImageCount,
        List<String> acceptedMimeTypes,
        long maxFileSizeBytes) {

    public static TemplateResponse from(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getTitle(),
                template.getCategory(),
                template.getCoverUrl(),
                template.getDescription(),
                template.getRequiredImageCount(),
                template.acceptedMimeTypeList(),
                template.getMaxFileSizeBytes());
    }
}
