package com.casecollaborative.engine.dto.websocket;

public record SesionResponse(
    Long id,
    Long proyectoId,
    String sessionToken,
    Long hostUsuarioId,
    Boolean activa
) {}
