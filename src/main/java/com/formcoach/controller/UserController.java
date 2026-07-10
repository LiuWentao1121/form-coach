package com.formcoach.controller;

import com.formcoach.common.Result;
import com.formcoach.entity.User;
import com.formcoach.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户", description = "个人中心与资料管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取个人资料（含统计、热力图、成就）")
    @GetMapping("/profile")
    public Result<?> getProfile() {
        Long userId = getCurrentUserId();
        return Result.ok(userService.getProfile(userId));
    }

    @Operation(summary = "更新个人资料（昵称、身高、体重等）")
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody User updates) {
        Long userId = getCurrentUserId();
        userService.updateProfile(userId, updates);
        return Result.ok();
    }

    @Operation(summary = "查询用户信息")
    @GetMapping("/{id}")
    public Result<?> getUser(@PathVariable Long id) {
        User user = userService.getById(id);
        if (user == null) {
            return com.formcoach.common.Result.fail(com.formcoach.common.ErrorCode.USER_NOT_FOUND);
        }
        user.setPassword(null);
        return Result.ok(user);
    }

    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
