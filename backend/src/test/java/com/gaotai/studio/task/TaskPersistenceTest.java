package com.gaotai.studio.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.gaotai.studio.common.ApiException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class TaskPersistenceTest {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private TaskQueue taskQueue;

    @Autowired
    private TaskQueryService taskQueryService;

    @Test
    void claimsOldestPendingAndSkipsTerminalTasks() {
        insert("older", "u-linxia", "PENDING", LocalDateTime.parse("2026-04-01T00:00:00"));
        insert("newer", "u-linxia", "PENDING", LocalDateTime.parse("2026-04-01T00:00:02"));
        insert("done", "u-linxia", "SUCCEEDED", LocalDateTime.parse("2026-03-01T00:00:00"));
        insert("failed", "u-linxia", "FAILED", LocalDateTime.parse("2026-03-02T00:00:00"));

        assertEquals("older", taskQueue.claimNext());
        assertEquals("PROCESSING", taskMapper.findById("older").getStatus());
        assertEquals("newer", taskQueue.claimNext());
        assertNull(taskQueue.claimNext());
    }

    @Test
    void completedTaskCannotReturnToProcessing() {
        insert("done", "u-linxia", "PROCESSING", LocalDateTime.parse("2026-04-01T00:00:00"));
        LocalDateTime finished = LocalDateTime.parse("2026-04-01T00:05:00");
        assertEquals(1, taskMapper.succeed("done", "RESULT_PACKAGING", "/media/results/done.png", finished));
        assertEquals(0, taskMapper.markStage("done", "ASSET_CHECK", finished.plusSeconds(5)));
        assertEquals(0, taskMapper.fail("done", "ASSET_CHECK", "不应覆盖成功", finished.plusSeconds(6)));
        TaskRecord stored = taskMapper.findById("done");
        assertEquals("SUCCEEDED", stored.getStatus());
        assertEquals("/media/results/done.png", stored.getResultUrl());
        assertNull(stored.getFailureReason());
    }

    @Test
    void queryReadsWithoutChangingStatus() {
        insert("task-1", "u-linxia", "PENDING", LocalDateTime.parse("2026-04-01T00:00:00"));
        TaskResponse first = taskQueryService.get("u-linxia", "task-1");
        TaskResponse second = taskQueryService.get("u-linxia", "task-1");
        assertEquals("PENDING", first.status());
        assertEquals(first, second);
        assertEquals("PENDING", taskMapper.findById("task-1").getStatus());
        ApiException hidden = assertThrows(ApiException.class, () -> taskQueryService.get("u-zhouyu", "task-1"));
        assertEquals(404, hidden.getStatus());
    }

    private void insert(String id, String userId, String status, LocalDateTime createdAt) {
        TaskRecord task = new TaskRecord();
        task.setId(id);
        task.setUserId(userId);
        task.setTemplateId("demo-postcard");
        task.setStatus(status);
        task.setCreatedAt(createdAt);
        task.setUpdatedAt(createdAt);
        if ("SUCCEEDED".equals(status)) {
            task.setStage("RESULT_PACKAGING");
            task.setResultUrl("/media/results/" + id + ".png");
            task.setFinishedAt(createdAt);
        }
        if ("FAILED".equals(status)) {
            task.setStage("SIMULATE_GENERATE");
            task.setFailureStage("SIMULATE_GENERATE");
            task.setFailureReason("模拟失败");
            task.setFinishedAt(createdAt);
        }
        taskMapper.insert(task);
    }
}
