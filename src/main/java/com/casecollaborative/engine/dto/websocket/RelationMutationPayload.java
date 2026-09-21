package com.casecollaborative.engine.dto.websocket;

public record RelationMutationPayload(
    Long relacionId,
    Long diagramaId,
    Long claseOrigenId,
    Long claseDestinoId,
    String nombre,
    String tipoRelacion,
    String cardinalidadOrigen,
    String cardinalidadDestino,
    String action
) {}
