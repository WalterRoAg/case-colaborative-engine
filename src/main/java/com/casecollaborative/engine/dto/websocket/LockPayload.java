package com.casecollaborative.engine.dto.websocket;

public record LockPayload(
    Long elementId,
    String elementType,
    Long lockedByUserId,
    String lockedByUserName,
    Boolean acquired
) {}
