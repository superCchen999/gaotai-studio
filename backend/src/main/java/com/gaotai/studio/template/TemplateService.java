package com.gaotai.studio.template;

import com.gaotai.studio.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TemplateService {

    private final TemplateMapper templateMapper;

    public TemplateService(TemplateMapper templateMapper) {
        this.templateMapper = templateMapper;
    }

    public TemplateListResponse list(String category) {
        String normalized = category == null ? null : category.trim();
        List<TemplateResponse> templates = templateMapper.findAll(normalized).stream()
                .map(TemplateResponse::from)
                .toList();
        return new TemplateListResponse(templateMapper.findCategories(), templates);
    }

    public TemplateResponse get(String id) {
        Template template = require(id);
        return TemplateResponse.from(template);
    }

    public Template require(String id) {
        if (id == null || id.isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "请指定模板");
        }
        Template template = templateMapper.findById(id.trim());
        if (template == null) {
            throw new ApiException(404, "TEMPLATE_NOT_FOUND", "模板不存在");
        }
        return template;
    }
}
