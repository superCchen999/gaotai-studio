package com.gaotai.studio.task;

import com.gaotai.studio.config.TimeSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskQueue {

    private final TaskMapper taskMapper;
    private final TimeSource timeSource;

    public TaskQueue(TaskMapper taskMapper, TimeSource timeSource) {
        this.taskMapper = taskMapper;
        this.timeSource = timeSource;
    }

    @Transactional
    public String claimNext() {
        String id = taskMapper.findOldestPendingId();
        if (id == null) {
            return null;
        }
        int updated = taskMapper.claim(id, timeSource.now());
        if (updated == 0) {
            return null;
        }
        return id;
    }
}
