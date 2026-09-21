package com.casecollaborative.engine.dto.websocket;

public record ClassMovePayload(
    Long claseId,
    Double posX,
    Double posY,
    Long version,
    Long clientTimestamp
) {}
