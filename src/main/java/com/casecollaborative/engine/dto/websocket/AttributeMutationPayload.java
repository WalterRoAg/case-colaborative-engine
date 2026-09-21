package com.casecollaborative.engine.dto.websocket;

public record AttributeMutationPayload(
    Long atributoId,
    Long claseId,
    String nombre,
    String tipoDato,
    String visibilidad,
    Boolean esPk,
    Integer orden,
    String action /* CREATE, UPDATE, DELETE */
) {}
