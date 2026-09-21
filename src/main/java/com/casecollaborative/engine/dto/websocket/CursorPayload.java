package com.casecollaborative.engine.dto.websocket;

public record CursorPayload(
    Long usuarioId,
    String nombreUsuario,
    Double x,
    Double y
) {}
