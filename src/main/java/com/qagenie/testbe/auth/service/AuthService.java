package com.qagenie.testbe.auth.service;

import com.qagenie.testbe.auth.dto.LoginRequest;
import com.qagenie.testbe.auth.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
