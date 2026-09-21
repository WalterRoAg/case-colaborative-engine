package com.casecollaborative.engine.dto.sync;

import java.util.List;

public record BatchSyncResponse(
    int totalRecibidos,
    int exitosos,
    int fallidos,
    List<SyncItemResult> resultados,
    Long serverTimestamp
) {}
