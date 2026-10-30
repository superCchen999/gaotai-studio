package com.gaotai.studio.task;

import com.gaotai.studio.user.CurrentUser;
import com.gaotai.studio.user.User;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskSubmissionService taskSubmissionService;
    private final TaskQueryService taskQueryService;

    public TaskController(TaskSubmissionService taskSubmissionService, TaskQueryService taskQueryService) {
        this.taskSubmissionService = taskSubmissionService;
        this.taskQueryService = taskQueryService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@CurrentUser User user, @RequestBody SubmitTaskRequest request) {
        return ResponseEntity.accepted().body(taskSubmissionService.submit(user, request));
    }

    @GetMapping
    public List<TaskResponse> list(@CurrentUser User user) {
        return taskQueryService.list(user.getId());
    }

    @GetMapping("/{id}")
    public TaskResponse get(@CurrentUser User user, @PathVariable String id) {
        return taskQueryService.get(user.getId(), id);
    }
}
