package com.casecollaborative.engine.dto.sync;

public record SyncItemRequest(
    String localId,             
    String sessionToken,        
    Long proyectoId,
    Long diagramaId,
    String tipoMutacion,        
    String payloadJson,         
    Long clientTimestamp,       
    String rawText
) {}
