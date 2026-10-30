package com.gaotai.studio.template;

import com.gaotai.studio.user.CurrentUser;
import com.gaotai.studio.user.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public TemplateListResponse list(
            @CurrentUser User user,
            @RequestParam(required = false) String category) {
        return templateService.list(category);
    }

    @GetMapping("/{id}")
    public TemplateResponse get(@CurrentUser User user, @PathVariable String id) {
        return templateService.get(id);
    }
}
