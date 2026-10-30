package com.gaotai.studio.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gaotai.studio.asset.Asset;
import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.support.MemoryDb;
import com.gaotai.studio.template.Template;
import com.gaotai.studio.template.TemplateService;
import com.gaotai.studio.user.User;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskSubmissionServiceTest {

    private final MemoryDb db = new MemoryDb();
    private int nudges;
    private TaskSubmissionService service;

    @BeforeEach
    void setUp() {
        db.addTemplate(template());
        db.addAsset(asset("a1", "u-linxia", "image/jpeg", 100));
        db.addAsset(asset("a2", "u-linxia", "image/png", 100));
        db.addAsset(asset("other", "u-zhouyu", "image/jpeg", 100));
        db.addAsset(asset("big", "u-linxia", "image/jpeg", 9_000_000));
        db.addAsset(asset("gif-like", "u-linxia", "image/gif", 100));
        service = new TaskSubmissionService(
                new TemplateService(db.templates()),
                db.assets(),
                db.files(),
                db.tasks(),
                new TimeSource(),
                () -> nudges++);
    }

    @Test
    void acceptsTaskWithoutStartingIt() {
        TaskResponse response = service.submit(user(), new SubmitTaskRequest("demo-postcard", List.of("a1", "a2")));
        assertEquals("PENDING", response.status());
        assertNull(response.stage());
        assertEquals("旅行明信片", response.templateTitle());
        assertEquals(List.of("a1", "a2"), db.findAssetIds(response.id()));
        assertEquals(1, nudges);
        assertTrue(db.stages.isEmpty());
    }

    @Test
    void rejectsInvalidAssetsAndDoesNotCreateTask() {
        assertCode("ASSET_COUNT", List.of("a1"));
        assertCode("DUPLICATE_ASSET", List.of("a1", "a1"));
        assertCode("ASSET_NOT_FOUND", List.of("a1", "other"));
        assertCode("ASSET_TOO_LARGE", List.of("a1", "big"));
        assertCode("ASSET_TYPE", List.of("a1", "gif-like"));
        assertCode("TEMPLATE_NOT_FOUND", "missing", List.of("a1", "a2"));
        db.deleteFile("a1.jpg");
        assertCode("ASSET_MISSING", List.of("a1", "a2"));
        assertEquals(0, nudges);
        assertTrue(db.findByUserId("u-linxia").isEmpty());
    }

    private void assertCode(String code, List<String> assetIds) {
        assertCode(code, "demo-postcard", assetIds);
    }

    private void assertCode(String code, String templateId, List<String> assetIds) {
        ApiException error = assertThrows(ApiException.class,
                () -> service.submit(user(), new SubmitTaskRequest(templateId, assetIds)));
        assertEquals(code, error.getCode());
    }

    private static User user() {
        User user = new User();
        user.setId("u-linxia");
        user.setDisplayName("林夏");
        return user;
    }

    private static Template template() {
        Template template = new Template();
        template.setId("demo-postcard");
        template.setTitle("旅行明信片");
        template.setCategory("创意图片");
        template.setRequiredImageCount(2);
        template.setAcceptedMimeTypes("image/jpeg,image/png");
        template.setMaxFileSizeBytes(5_242_880);
        return template;
    }

    private static Asset asset(String id, String userId, String mime, long size) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setUserId(userId);
        asset.setMimeType(mime);
        asset.setSizeBytes(size);
        asset.setStorageName(id + ".jpg");
        asset.setOriginalFilename(id + ".jpg");
        return asset;
    }
}
