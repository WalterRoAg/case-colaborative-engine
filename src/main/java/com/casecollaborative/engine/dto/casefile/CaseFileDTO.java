package com.casecollaborative.engine.dto.casefile;

import java.util.List;

public record CaseFileDTO(
    String nombreDiagrama,
    Integer version,
    Double zoomCanvas,
    Double panX,
    Double panY,
    List<ClaseDTO> clases,
    List<RelacionDTO> relaciones
) {}
