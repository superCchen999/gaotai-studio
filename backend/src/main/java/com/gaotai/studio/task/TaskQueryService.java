package com.gaotai.studio.task;

import com.gaotai.studio.common.ApiException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TaskQueryService {

    private final TaskMapper taskMapper;

    public TaskQueryService(TaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    public TaskResponse get(String userId, String taskId) {
        TaskRecord task = requireOwned(userId, taskId);
        return TaskResponses.from(task);
    }

    public List<TaskResponse> list(String userId) {
        return taskMapper.findByUserId(userId).stream().map(TaskResponses::from).toList();
    }

    private TaskRecord requireOwned(String userId, String taskId) {
        if (taskId == null || !taskId.matches("[A-Za-z0-9-]{1,64}")) {
            throw new ApiException(404, "TASK_NOT_FOUND", "任务不存在");
        }
        TaskRecord task = taskMapper.findById(taskId);
        if (task == null || !userId.equals(task.getUserId())) {
            throw new ApiException(404, "TASK_NOT_FOUND", "任务不存在");
        }
        return task;
    }
}
