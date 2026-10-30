package com.gaotai.studio.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.support.MemoryDb;
import com.gaotai.studio.template.Template;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskQueryServiceTest {

    private final MemoryDb db = new MemoryDb();
    private TaskQueryService service;

    @BeforeEach
    void setUp() {
        Template template = new Template();
        template.setId("demo-postcard");
        template.setTitle("旅行明信片");
        db.addTemplate(template);
        service = new TaskQueryService(db.tasks());
    }

    @Test
    void readingDoesNotAdvanceOrReopen() {
        db.tasks().insert(task("task-1", "u-linxia", "PROCESSING", "ASSET_CHECK"));
        db.tasks().insert(task("task-2", "u-linxia", "SUCCEEDED", "RESULT_PACKAGING"));
        int writes = db.statusWrites;
        TaskResponse processing = service.get("u-linxia", "task-1");
        TaskResponse again = service.get("u-linxia", "task-1");
        TaskResponse succeeded = service.get("u-linxia", "task-2");
        assertEquals("PROCESSING", processing.status());
        assertEquals("ASSET_CHECK", again.stage());
        assertEquals("SUCCEEDED", succeeded.status());
        assertEquals(writes, db.statusWrites);
        assertEquals("PROCESSING", db.findTask("task-1").getStatus());
        assertEquals("SUCCEEDED", db.findTask("task-2").getStatus());
    }

    @Test
    void hidesOtherUsersTasks() {
        db.tasks().insert(task("task-1", "u-linxia", "PENDING", null));
        ApiException missing = assertThrows(ApiException.class, () -> service.get("u-zhouyu", "task-1"));
        assertEquals(404, missing.getStatus());
        assertEquals(0, service.list("u-zhouyu").size());
        assertEquals(1, service.list("u-linxia").size());
    }

    private static TaskRecord task(String id, String userId, String status, String stage) {
        TaskRecord task = new TaskRecord();
        task.setId(id);
        task.setUserId(userId);
        task.setTemplateId("demo-postcard");
        task.setStatus(status);
        task.setStage(stage);
        task.setResultUrl("SUCCEEDED".equals(status) ? "/media/results/" + id + ".png" : null);
        task.setCreatedAt(LocalDateTime.parse("2026-04-01T00:00:00"));
        task.setUpdatedAt(LocalDateTime.parse("2026-04-01T00:00:01"));
        if ("SUCCEEDED".equals(status) || "FAILED".equals(status)) {
            task.setFinishedAt(LocalDateTime.parse("2026-04-01T00:00:02"));
        }
        return task;
    }
}
