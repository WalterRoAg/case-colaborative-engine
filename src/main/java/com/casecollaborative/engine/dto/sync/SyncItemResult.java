package com.casecollaborative.engine.dto.sync;

public record SyncItemResult(
    String localId,
    boolean exitoso,
    String mensaje
) {}
