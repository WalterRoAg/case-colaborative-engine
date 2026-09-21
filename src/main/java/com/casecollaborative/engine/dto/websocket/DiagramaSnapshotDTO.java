package com.casecollaborative.engine.dto.websocket;

import com.casecollaborative.engine.model.entity.Clase;
import com.casecollaborative.engine.model.entity.RelacionClase;
import java.util.List;

public record DiagramaSnapshotDTO(
    Long diagramaId,
    Long proyectoId,
    String nombre,
    Integer version,
    Double zoomCanvas,
    Double panX,
    Double panY,
    List<Clase> clases,
    List<RelacionClase> relaciones
) {}
