package com.casecollaborative.engine.controller;

import com.casecollaborative.engine.dto.MetricaKpiDTO;
import com.casecollaborative.engine.service.MetricasKpiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/proyectos/{id}/metricas")
public class MetricasController {

    private final MetricasKpiService metricasKpiService;

    public MetricasController(MetricasKpiService metricasKpiService) {
        this.metricasKpiService = metricasKpiService;
    }

    @GetMapping
    public ResponseEntity<MetricaKpiDTO> obtenerMetricas(@PathVariable Long id) {
        // En una app real, aquí se leería el último guardado sin recalcular si no es necesario.
        // Pero para el alcance de este monitor, podemos recalcular o simplemente consultar BD.
        // Optamos por recalcular para tener datos frescos siempre que se consulte GET.
        MetricaKpiDTO metricas = metricasKpiService.calcularYRegistrarMetricas(id);
        return ResponseEntity.ok(metricas);
    }

    @PostMapping("/calcular")
    public ResponseEntity<MetricaKpiDTO> calcularMetricas(@PathVariable Long id) {
        MetricaKpiDTO metricas = metricasKpiService.calcularYRegistrarMetricas(id);
        return ResponseEntity.ok(metricas);
    }
}
