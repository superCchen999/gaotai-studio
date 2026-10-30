package com.gaotai.studio.user;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping
    public List<UserResponse> list() {
        return userMapper.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getDisplayName()))
                .toList();
    }
}
