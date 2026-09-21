package com.casecollaborative.engine.service;

import com.casecollaborative.engine.dto.request.LoginRequest;
import com.casecollaborative.engine.dto.request.RegistroRequest;
import com.casecollaborative.engine.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse registrar(RegistroRequest request);
    AuthResponse login(LoginRequest request);
}
