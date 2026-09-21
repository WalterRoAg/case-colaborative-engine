package com.casecollaborative.engine.dto;

import java.util.List;

public record MetricaKpiDTO(
    Long proyectoId,
    Integer tiempoModeladoSeg,
    Double elementosPorMinuto,
    Double acoplamientoCbo,
    Integer violacionesUml,
    Double tasaCompilacion,
    List<String> detallesViolaciones
) {}
