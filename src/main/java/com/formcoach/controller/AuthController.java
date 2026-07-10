package com.formcoach.controller;

import com.formcoach.annotation.RateLimit;
import com.formcoach.common.Result;
import com.formcoach.dto.LoginRequest;
import com.formcoach.dto.RegisterRequest;
import com.formcoach.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证", description = "用户注册与登录")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<?> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(authService.register(request));
    }

    @Operation(summary = "用户登录")
    @RateLimit(maxRequests = 5, duration = 60, key = "login")  // 每分钟最多5次
    @PostMapping("/login")
    public Result<?> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }
}
