package com.casecollaborative.engine.dto.sync;

import java.util.List;

public record BatchSyncRequest(
    List<SyncItemRequest> mutaciones
) {}
