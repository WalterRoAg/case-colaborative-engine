package com.casecollaborative.engine.dto.casefile;

public record AtributoDTO(
    Long idOriginal,
    String nombre,
    String tipoDato,
    String visibilidad,
    Boolean esPk,
    Integer orden
) {}
