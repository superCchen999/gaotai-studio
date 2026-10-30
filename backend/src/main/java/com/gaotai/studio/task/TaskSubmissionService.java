package com.gaotai.studio.task;

import com.gaotai.studio.asset.Asset;
import com.gaotai.studio.asset.AssetMapper;
import com.gaotai.studio.asset.UploadStore;
import com.gaotai.studio.common.ApiException;
import com.gaotai.studio.config.TimeSource;
import com.gaotai.studio.template.Template;
import com.gaotai.studio.template.TemplateService;
import com.gaotai.studio.user.User;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TaskSubmissionService {

    private final TemplateService templateService;
    private final AssetMapper assetMapper;
    private final UploadStore uploadStore;
    private final TaskMapper taskMapper;
    private final TimeSource timeSource;
    private final TaskWakeup taskWakeup;

    public TaskSubmissionService(
            TemplateService templateService,
            AssetMapper assetMapper,
            UploadStore uploadStore,
            TaskMapper taskMapper,
            TimeSource timeSource,
            TaskWakeup taskWakeup) {
        this.templateService = templateService;
        this.assetMapper = assetMapper;
        this.uploadStore = uploadStore;
        this.taskMapper = taskMapper;
        this.timeSource = timeSource;
        this.taskWakeup = taskWakeup;
    }

    @Transactional
    public TaskResponse submit(User user, SubmitTaskRequest request) {
        if (request == null || request.templateId() == null || request.templateId().isBlank()) {
            throw new ApiException(400, "INVALID_REQUEST", "请指定模板");
        }
        if (request.assetIds() == null || request.assetIds().isEmpty()) {
            throw new ApiException(400, "INVALID_ASSETS", "请上传模板要求的图片");
        }
        if (request.assetIds().size() > 10) {
            throw new ApiException(400, "INVALID_ASSETS", "素材数量超出限制");
        }
        Template template = templateService.require(request.templateId());
        List<String> assetIds = new ArrayList<>();
        for (String raw : request.assetIds()) {
            if (raw == null || raw.isBlank()) {
                throw new ApiException(400, "INVALID_ASSETS", "素材编号无效");
            }
            assetIds.add(raw.trim());
        }
        Set<String> distinct = new HashSet<>(assetIds);
        if (distinct.size() != assetIds.size()) {
            throw new ApiException(400, "DUPLICATE_ASSET", "同一张素材不能重复使用");
        }
        if (assetIds.size() != template.getRequiredImageCount()) {
            throw new ApiException(400, "ASSET_COUNT", "这个模板需要 " + template.getRequiredImageCount() + " 张图片");
        }
        List<String> allowed = template.acceptedMimeTypeList();
        for (String assetId : assetIds) {
            Asset asset = assetMapper.findById(assetId);
            if (asset == null || !user.getId().equals(asset.getUserId())) {
                throw new ApiException(400, "ASSET_NOT_FOUND", "素材不存在或不属于当前用户");
            }
            if (!allowed.contains(asset.getMimeType())) {
                throw new ApiException(400, "ASSET_TYPE", "素材类型不符合模板要求");
            }
            if (asset.getSizeBytes() > template.getMaxFileSizeBytes()) {
                throw new ApiException(400, "ASSET_TOO_LARGE", "素材超过模板大小限制");
            }
            if (!uploadStore.exists(asset.getStorageName())) {
                throw new ApiException(400, "ASSET_MISSING", "素材文件不存在，请重新上传");
            }
        }

        var now = timeSource.now();
        TaskRecord task = new TaskRecord();
        task.setId(UUID.randomUUID().toString());
        task.setUserId(user.getId());
        task.setTemplateId(template.getId());
        task.setTemplateTitle(template.getTitle());
        task.setStatus(TaskStatus.PENDING.name());
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        taskMapper.insert(task);
        for (int index = 0; index < assetIds.size(); index++) {
            taskMapper.insertTaskAsset(task.getId(), assetIds.get(index), index);
        }
        wakeAfterCommit();
        return TaskResponses.from(task);
    }

    private void wakeAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    taskWakeup.nudge();
                }
            });
            return;
        }
        taskWakeup.nudge();
    }
}
