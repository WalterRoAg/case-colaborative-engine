package com.casecollaborative.engine.dto.websocket;

public record SocketEventEnvelope(
    EventType eventType,
    String sessionToken,
    Long senderId,
    String senderName,
    Long timestamp,
    Object data
) {}
