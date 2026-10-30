package com.gaotai.studio.support;

import com.gaotai.studio.asset.Asset;
import com.gaotai.studio.asset.AssetMapper;
import com.gaotai.studio.asset.UploadStore;
import com.gaotai.studio.task.TaskMapper;
import com.gaotai.studio.task.TaskRecord;
import com.gaotai.studio.task.TaskStatus;
import com.gaotai.studio.template.Template;
import com.gaotai.studio.template.TemplateMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MemoryDb {

    public int statusWrites;
    public final List<String> stages = new ArrayList<>();
    private final Map<String, TaskRecord> tasks = new HashMap<>();
    private final Map<String, List<String>> taskAssets = new HashMap<>();
    private final Map<String, Asset> assets = new HashMap<>();
    private final Map<String, Template> templates = new HashMap<>();
    private final Set<String> files = new HashSet<>();
    private final TaskMapper taskMapper = new TaskStore();
    private final AssetMapper assetMapper = new AssetStore();
    private final TemplateMapper templateMapper = new TemplateStore();
    private final UploadStore uploadStore = new FileStore();

    public TaskMapper tasks() {
        return taskMapper;
    }

    public AssetMapper assets() {
        return assetMapper;
    }

    public TemplateMapper templates() {
        return templateMapper;
    }

    public UploadStore files() {
        return uploadStore;
    }

    public void addTemplate(Template template) {
        templates.put(template.getId(), template);
    }

    public void addAsset(Asset asset) {
        assets.put(asset.getId(), asset);
        files.add(asset.getStorageName());
    }

    public TaskRecord findTask(String id) {
        return taskMapper.findById(id);
    }

    public List<String> findAssetIds(String taskId) {
        return taskMapper.findAssetIds(taskId);
    }

    public List<TaskRecord> findByUserId(String userId) {
        return taskMapper.findByUserId(userId);
    }

    public void deleteFile(String storageName) {
        uploadStore.delete(storageName);
    }

    public boolean fileExists(String storageName) {
        return uploadStore.exists(storageName);
    }

    private TaskRecord copyTask(TaskRecord source) {
        TaskRecord copy = new TaskRecord();
        copy.setId(source.getId());
        copy.setUserId(source.getUserId());
        copy.setTemplateId(source.getTemplateId());
        copy.setStatus(source.getStatus());
        copy.setStage(source.getStage());
        copy.setFailureStage(source.getFailureStage());
        copy.setFailureReason(source.getFailureReason());
        copy.setResultUrl(source.getResultUrl());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setFinishedAt(source.getFinishedAt());
        Template template = templates.get(source.getTemplateId());
        copy.setTemplateTitle(template == null ? source.getTemplateTitle() : template.getTitle());
        return copy;
    }

    private final class TaskStore implements TaskMapper {
        @Override
        public int insert(TaskRecord task) {
            tasks.put(task.getId(), copyTask(task));
            statusWrites++;
            return 1;
        }

        @Override
        public int insertTaskAsset(String taskId, String assetId, int sortIndex) {
            List<String> ids = taskAssets.computeIfAbsent(taskId, ignored -> new ArrayList<>());
            while (ids.size() <= sortIndex) {
                ids.add(null);
            }
            ids.set(sortIndex, assetId);
            return 1;
        }

        @Override
        public TaskRecord findById(String id) {
            TaskRecord task = tasks.get(id);
            return task == null ? null : copyTask(task);
        }

        @Override
        public List<TaskRecord> findByUserId(String userId) {
            return tasks.values().stream()
                    .filter(task -> userId.equals(task.getUserId()))
                    .sorted(Comparator.comparing(TaskRecord::getCreatedAt).reversed()
                            .thenComparing(TaskRecord::getId, Comparator.reverseOrder()))
                    .map(MemoryDb.this::copyTask)
                    .toList();
        }

        @Override
        public String findOldestPendingId() {
            return tasks.values().stream()
                    .filter(task -> TaskStatus.PENDING.name().equals(task.getStatus()))
                    .min(Comparator.comparing(TaskRecord::getCreatedAt).thenComparing(TaskRecord::getId))
                    .map(TaskRecord::getId)
                    .orElse(null);
        }

        @Override
        public int claim(String id, LocalDateTime updatedAt) {
            statusWrites++;
            TaskRecord task = tasks.get(id);
            if (task == null || !TaskStatus.PENDING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(TaskStatus.PROCESSING.name());
            task.setUpdatedAt(updatedAt);
            return 1;
        }

        @Override
        public int markStage(String id, String stage, LocalDateTime updatedAt) {
            statusWrites++;
            TaskRecord task = tasks.get(id);
            if (task == null || !TaskStatus.PROCESSING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStage(stage);
            task.setUpdatedAt(updatedAt);
            stages.add(stage);
            return 1;
        }

        @Override
        public int fail(String id, String stage, String reason, LocalDateTime updatedAt) {
            statusWrites++;
            TaskRecord task = tasks.get(id);
            if (task == null || !TaskStatus.PROCESSING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(TaskStatus.FAILED.name());
            task.setStage(stage);
            task.setFailureStage(stage);
            task.setFailureReason(reason);
            task.setUpdatedAt(updatedAt);
            task.setFinishedAt(updatedAt);
            return 1;
        }

        @Override
        public int succeed(String id, String stage, String resultUrl, LocalDateTime updatedAt) {
            statusWrites++;
            TaskRecord task = tasks.get(id);
            if (task == null || !TaskStatus.PROCESSING.name().equals(task.getStatus())) {
                return 0;
            }
            task.setStatus(TaskStatus.SUCCEEDED.name());
            task.setStage(stage);
            task.setResultUrl(resultUrl);
            task.setFailureStage(null);
            task.setFailureReason(null);
            task.setUpdatedAt(updatedAt);
            task.setFinishedAt(updatedAt);
            return 1;
        }

        @Override
        public List<String> findAssetIds(String taskId) {
            List<String> ids = taskAssets.get(taskId);
            if (ids == null) {
                return List.of();
            }
            return ids.stream().filter(id -> id != null).toList();
        }
    }

    private final class AssetStore implements AssetMapper {
        @Override
        public int insert(Asset asset) {
            assets.put(asset.getId(), asset);
            return 1;
        }

        @Override
        public Asset findById(String id) {
            return assets.get(id);
        }
    }

    private final class TemplateStore implements TemplateMapper {
        @Override
        public Template findById(String id) {
            return templates.get(id);
        }

        @Override
        public List<Template> findAll(String category) {
            return templates.values().stream()
                    .filter(template -> category == null || category.isBlank() || category.equals(template.getCategory()))
                    .sorted(Comparator.comparingInt(Template::getSortIndex))
                    .toList();
        }

        @Override
        public List<String> findCategories() {
            return templates.values().stream()
                    .sorted(Comparator.comparingInt(Template::getSortIndex))
                    .map(Template::getCategory)
                    .distinct()
                    .toList();
        }
    }

    private final class FileStore implements UploadStore {
        @Override
        public boolean exists(String storageName) {
            return files.contains(storageName);
        }

        @Override
        public void write(String storageName, byte[] bytes) {
            files.add(storageName);
        }

        @Override
        public void delete(String storageName) {
            files.remove(storageName);
        }
    }
}
