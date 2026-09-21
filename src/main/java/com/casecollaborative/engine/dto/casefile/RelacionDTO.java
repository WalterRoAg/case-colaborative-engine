package com.casecollaborative.engine.dto.casefile;

public record RelacionDTO(
    Long origenIdOriginal,
    Long destinoIdOriginal,
    String tipoRelacion,
    String cardinalidadOrigen,
    String cardinalidadDestino
) {}
