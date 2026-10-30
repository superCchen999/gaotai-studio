package com.gaotai.studio.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gaotai.studio.asset.Asset;
import com.gaotai.studio.config.StudioProperties;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.support.MemoryDb;
import com.gaotai.studio.template.Template;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskProcessorTest {

    private final MemoryDb db = new MemoryDb();
    private final List<Long> sleeps = new ArrayList<>();
    private final List<String> stagesAtSleep = new ArrayList<>();
    private final List<String> written = new ArrayList<>();
    private SimulatorSettings settings;
    private TaskProcessor processor;

    @BeforeEach
    void setUp() {
        Template template = template();
        db.addTemplate(template);
        db.addAsset(asset("a1"));
        db.addAsset(asset("a2"));
        settings = settings(0, "NONE");
        processor = new TaskProcessor(
                db.tasks(),
                db.assets(),
                db.templates(),
                db.files(),
                (taskId, title) -> {
                    written.add(taskId);
                    return "/media/results/" + taskId + ".png";
                },
                settings,
                millis -> {
                    sleeps.add(millis);
                    stagesAtSleep.add(db.findTask("task-1").getStage());
                },
                new TimeSource());
    }

    @Test
    void runsStagesInOrderAndPublishesResult() {
        seedProcessing();
        processor.process("task-1");

        assertEquals(List.of("ASSET_CHECK", "SIMULATE_GENERATE", "RESULT_PACKAGING"), db.stages);
        assertEquals(db.stages, stagesAtSleep);
        assertEquals(List.of(0L, 0L, 0L), sleeps);
        assertEquals(List.of("task-1"), written);
        TaskRecord done = db.findTask("task-1");
        assertEquals("SUCCEEDED", done.getStatus());
        assertEquals("RESULT_PACKAGING", done.getStage());
        assertEquals("/media/results/task-1.png", done.getResultUrl());
        assertNull(done.getFailureReason());
    }

    @Test
    void failureStopsLaterStages() {
        seedProcessing();
        settings.update(0L, "SIMULATE_GENERATE");
        processor.process("task-1");

        assertEquals(List.of("ASSET_CHECK", "SIMULATE_GENERATE"), db.stages);
        assertTrue(written.isEmpty());
        TaskRecord done = db.findTask("task-1");
        assertEquals("FAILED", done.getStatus());
        assertEquals("SIMULATE_GENERATE", done.getFailureStage());
        assertTrue(done.getFailureReason().contains("模拟生成失败"));
        assertNull(done.getResultUrl());
    }

    @Test
    void failureAtFirstAndLastStageDoesNotPublishResult() {
        seedProcessing();
        settings.update(0L, "ASSET_CHECK");
        processor.process("task-1");
        assertEquals(List.of("ASSET_CHECK"), db.stages);
        assertEquals("FAILED", db.findTask("task-1").getStatus());

        db.stages.clear();
        written.clear();
        seedProcessing("task-2");
        settings.update(0L, "RESULT_PACKAGING");
        processor = processorWithSleeper("task-2");
        processor.process("task-2");
        assertEquals(List.of("ASSET_CHECK", "SIMULATE_GENERATE", "RESULT_PACKAGING"), db.stages);
        assertTrue(written.isEmpty());
        assertEquals("FAILED", db.findTask("task-2").getStatus());
        assertEquals("RESULT_PACKAGING", db.findTask("task-2").getFailureStage());
    }

    @Test
    void missingFileFailsAssetCheckAndSkipsGeneration() {
        seedProcessing();
        db.deleteFile("a1.jpg");
        processor.process("task-1");
        assertEquals(List.of("ASSET_CHECK"), db.stages);
        assertTrue(written.isEmpty());
        TaskRecord done = db.findTask("task-1");
        assertEquals("FAILED", done.getStatus());
        assertEquals("ASSET_CHECK", done.getFailureStage());
        assertTrue(done.getFailureReason().contains("不存在"));
    }

    @Test
    void pendingTaskIsNotAdvancedByTheProcessor() {
        TaskRecord pending = processingRecord("task-1");
        pending.setStatus("PENDING");
        pending.setStage(null);
        db.tasks().insert(pending);
        db.tasks().insertTaskAsset("task-1", "a1", 0);
        db.tasks().insertTaskAsset("task-1", "a2", 1);
        int writes = db.statusWrites;
        processor.process("task-1");
        assertEquals(writes, db.statusWrites);
        assertEquals("PENDING", db.findTask("task-1").getStatus());
        assertTrue(db.stages.isEmpty());
    }

    @Test
    void completedTaskIsNotReopened() {
        seedProcessing();
        processor.process("task-1");
        int writes = db.statusWrites;
        processor.process("task-1");
        assertEquals(writes, db.statusWrites);
        assertEquals("SUCCEEDED", db.findTask("task-1").getStatus());
    }

    private void seedProcessing() {
        seedProcessing("task-1");
    }

    private void seedProcessing(String id) {
        db.tasks().insert(processingRecord(id));
        db.tasks().insertTaskAsset(id, "a1", 0);
        db.tasks().insertTaskAsset(id, "a2", 1);
    }

    private TaskProcessor processorWithSleeper(String taskId) {
        return new TaskProcessor(
                db.tasks(),
                db.assets(),
                db.templates(),
                db.files(),
                (id, title) -> {
                    written.add(id);
                    return "/media/results/" + id + ".png";
                },
                settings,
                millis -> stagesAtSleep.add(db.findTask(taskId).getStage()),
                new TimeSource());
    }

    private static TaskRecord processingRecord(String id) {
        TaskRecord task = new TaskRecord();
        task.setId(id);
        task.setUserId("u-linxia");
        task.setTemplateId("demo-postcard");
        task.setStatus("PROCESSING");
        task.setCreatedAt(LocalDateTime.parse("2026-04-01T00:00:00"));
        task.setUpdatedAt(LocalDateTime.parse("2026-04-01T00:00:00"));
        return task;
    }

    private static Template template() {
        Template template = new Template();
        template.setId("demo-postcard");
        template.setTitle("旅行明信片");
        template.setCategory("创意图片");
        template.setRequiredImageCount(2);
        template.setAcceptedMimeTypes("image/jpeg,image/png");
        template.setMaxFileSizeBytes(5242880);
        template.setSortIndex(1);
        return template;
    }

    private static Asset asset(String id) {
        Asset asset = new Asset();
        asset.setId(id);
        asset.setUserId("u-linxia");
        asset.setOriginalFilename(id + ".jpg");
        asset.setMimeType("image/jpeg");
        asset.setSizeBytes(1200);
        asset.setStorageName(id + ".jpg");
        return asset;
    }

    private static SimulatorSettings settings(long delay, String failStage) {
        StudioProperties properties = new StudioProperties();
        properties.getSimulator().setStepDelayMs(delay);
        properties.getSimulator().setFailStage(failStage);
        return new SimulatorSettings(properties);
    }
}
