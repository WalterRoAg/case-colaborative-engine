package com.casecollaborative.engine.dto.response;

import java.util.List;

public record AuthResponse(
        String token,
        String tokenType,
        Long usuarioId,
        String nombre,
        String correo,
        List<String> roles
) {}
