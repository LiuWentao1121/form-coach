package com.formcoach.service;

import com.formcoach.dto.LoginRequest;
import com.formcoach.dto.RegisterRequest;

import java.util.Map;

public interface AuthService {

    /**
     * Register a new user
     * @return the new user's basic info
     */
    Map<String, Object> register(RegisterRequest request);

    /**
     * Login and return JWT token
     * @return token + user info
     */
    Map<String, Object> login(LoginRequest request);
}
