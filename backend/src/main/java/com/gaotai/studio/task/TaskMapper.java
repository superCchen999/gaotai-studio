package com.gaotai.studio.task;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TaskMapper {

    int insert(TaskRecord task);

    int insertTaskAsset(
            @Param("taskId") String taskId,
            @Param("assetId") String assetId,
            @Param("sortIndex") int sortIndex);

    TaskRecord findById(@Param("id") String id);

    List<TaskRecord> findByUserId(@Param("userId") String userId);

    String findOldestPendingId();

    int claim(@Param("id") String id, @Param("updatedAt") LocalDateTime updatedAt);

    int markStage(
            @Param("id") String id,
            @Param("stage") String stage,
            @Param("updatedAt") LocalDateTime updatedAt);

    int fail(
            @Param("id") String id,
            @Param("stage") String stage,
            @Param("reason") String reason,
            @Param("updatedAt") LocalDateTime updatedAt);

    int succeed(
            @Param("id") String id,
            @Param("stage") String stage,
            @Param("resultUrl") String resultUrl,
            @Param("updatedAt") LocalDateTime updatedAt);

    List<String> findAssetIds(@Param("taskId") String taskId);
}
