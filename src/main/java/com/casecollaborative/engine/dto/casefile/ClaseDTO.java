package com.casecollaborative.engine.dto.casefile;

import java.util.List;

public record ClaseDTO(
    Long idOriginal,
    String nombre,
    String visibilidad,
    Double posX,
    Double posY,
    List<AtributoDTO> atributos
) {}
