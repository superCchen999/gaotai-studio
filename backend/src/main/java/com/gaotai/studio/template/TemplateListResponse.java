package com.gaotai.studio.template;

import java.util.List;

public record TemplateListResponse(List<String> categories, List<TemplateResponse> templates) {
}
