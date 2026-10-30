package com.gaotai.studio.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaotai.studio.common.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class UserFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = "currentUser";

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    public UserFilter(UserMapper userMapper, ObjectMapper objectMapper) {
        this.userMapper = userMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/") || isPublic(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            writeError(response, 401, "USER_REQUIRED", "请先选择用户");
            return;
        }
        User user = userMapper.findById(userId.trim());
        if (user == null) {
            writeError(response, 401, "USER_NOT_FOUND", "用户不存在");
            return;
        }
        request.setAttribute(ATTRIBUTE, user);
        filterChain.doFilter(request, response);
    }

    private boolean isPublic(HttpServletRequest request) {
        if (!"GET".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String uri = request.getRequestURI();
        return "/api/health".equals(uri) || "/api/users".equals(uri);
    }

    private void writeError(HttpServletResponse response, int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), new ErrorResponse(code, message));
    }
}
