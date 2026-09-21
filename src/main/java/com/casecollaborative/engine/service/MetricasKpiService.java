package com.casecollaborative.engine.service;

import com.casecollaborative.engine.dto.MetricaKpiDTO;
import com.casecollaborative.engine.model.entity.*;
import com.casecollaborative.engine.repository.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.*;

@Service
public class MetricasKpiService {

    private final ProyectoRepository proyectoRepository;
    private final DiagramaUmlRepository diagramaRepository;
    private final ClaseRepository claseRepository;
    private final RelacionClaseRepository relacionRepository;
    private final MetricaKpiRepository metricaKpiRepository;
    private final SesionColaborativaRepository sesionRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MetricasKpiService(ProyectoRepository proyectoRepository,
                              DiagramaUmlRepository diagramaRepository,
                              ClaseRepository claseRepository,
                              RelacionClaseRepository relacionRepository,
                              MetricaKpiRepository metricaKpiRepository,
                              SesionColaborativaRepository sesionRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.proyectoRepository = proyectoRepository;
        this.diagramaRepository = diagramaRepository;
        this.claseRepository = claseRepository;
        this.relacionRepository = relacionRepository;
        this.metricaKpiRepository = metricaKpiRepository;
        this.sesionRepository = sesionRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public MetricaKpiDTO calcularYRegistrarMetricas(Long proyectoId) {
        Proyecto proyecto = proyectoRepository.findById(proyectoId)
                .orElseThrow(() -> new RuntimeException("Proyecto no encontrado"));

        DiagramaUml diagrama = diagramaRepository.findByProyectoId(proyectoId).orElse(null);
        if (diagrama == null) {
            return emitAndReturn(new MetricaKpiDTO(proyectoId, 0, 0.0, 0.0, 0, 0.0, List.of("Diagrama vacío")));
        }

        List<Clase> clases = claseRepository.findByDiagramaId(diagrama.getId());
        List<RelacionClase> relaciones = relacionRepository.findByDiagramaId(diagrama.getId());

        int N = clases.size();
        if (N == 0) {
            return emitAndReturn(new MetricaKpiDTO(proyectoId, 0, 0.0, 0.0, 0, 0.0, List.of("Sin clases")));
        }

        // 1. Cálculo CBO (Coupling Between Objects)
        Map<Long, Set<Long>> conexionesPorClase = new HashMap<>();
        for (Clase c : clases) {
            conexionesPorClase.put(c.getId(), new HashSet<>());
        }

        for (RelacionClase rel : relaciones) {
            conexionesPorClase.get(rel.getClaseOrigen().getId()).add(rel.getClaseDestino().getId());
            conexionesPorClase.get(rel.getClaseDestino().getId()).add(rel.getClaseOrigen().getId());
        }

        double sumCbo = 0;
        for (Set<Long> conex : conexionesPorClase.values()) {
            sumCbo += conex.size();
        }
        double cboPromedio = sumCbo / N;

        // 2. Violaciones UML
        List<String> advertencias = new ArrayList<>();
        int violaciones = 0;

        // a) Clases huérfanas (sin conexiones)
        for (Clase c : clases) {
            if (conexionesPorClase.get(c.getId()).isEmpty()) {
                violaciones++;
                advertencias.add("Clase huérfana (aislada): " + c.getNombre());
            }
        }

        // b) Nombres duplicados
        Set<String> nombres = new HashSet<>();
        for (Clase c : clases) {
            if (!nombres.add(c.getNombre().toLowerCase())) {
                violaciones++;
                advertencias.add("Nombre de clase duplicado: " + c.getNombre());
            }
        }

        // c) Ciclos de herencia (DFS)
        Map<Long, List<Long>> herenciaGraph = new HashMap<>();
        for (RelacionClase rel : relaciones) {
            if ("HERENCIA".equalsIgnoreCase(rel.getTipoRelacion())) {
                herenciaGraph.computeIfAbsent(rel.getClaseOrigen().getId(), k -> new ArrayList<>()).add(rel.getClaseDestino().getId());
            }
        }

        for (Clase c : clases) {
            if (hasCycle(c.getId(), herenciaGraph, new HashSet<>(), new HashSet<>())) {
                violaciones++;
                advertencias.add("Referencia circular de herencia detectada en: " + c.getNombre());
            }
        }

        // 3. Métricas de Productividad
        SesionColaborativa sesion = sesionRepository.findByProyectoIdAndActivaTrue(proyectoId).orElse(null);
        int tiempoModeladoSeg = 0;
        if (sesion != null && sesion.getFechaInicio() != null) {
            tiempoModeladoSeg = (int) Duration.between(sesion.getFechaInicio(), ZonedDateTime.now()).getSeconds();
        }

        double elementosPorMin = 0.0;
        int totalElementos = N + relaciones.size();
        if (tiempoModeladoSeg > 0) {
            elementosPorMin = (totalElementos / (double) tiempoModeladoSeg) * 60.0;
        }

        double tasaCompilacion = Math.max(0.0, 100.0 - (violaciones * 5.0)); // Simple heuristic

        // 4. Persistir
        MetricaKpi metrica = metricaKpiRepository.findByProyectoId(proyectoId).orElse(new MetricaKpi());
        metrica.setProyecto(proyecto);
        metrica.setTiempoModeladoSeg(tiempoModeladoSeg);
        metrica.setElementosPorMinuto(BigDecimal.valueOf(elementosPorMin).setScale(2, RoundingMode.HALF_UP));
        metrica.setAcoplamientoCbo(BigDecimal.valueOf(cboPromedio).setScale(2, RoundingMode.HALF_UP));
        metrica.setViolacionesUml(violaciones);
        metrica.setTasaCompilacion(BigDecimal.valueOf(tasaCompilacion).setScale(2, RoundingMode.HALF_UP));
        
        metricaKpiRepository.save(metrica);

        MetricaKpiDTO dto = new MetricaKpiDTO(
                proyectoId,
                tiempoModeladoSeg,
                metrica.getElementosPorMinuto().doubleValue(),
                metrica.getAcoplamientoCbo().doubleValue(),
                violaciones,
                metrica.getTasaCompilacion().doubleValue(),
                advertencias
        );

        return emitAndReturn(dto);
    }

    private boolean hasCycle(Long node, Map<Long, List<Long>> graph, Set<Long> visited, Set<Long> recStack) {
        if (recStack.contains(node)) return true;
        if (visited.contains(node)) return false;

        visited.add(node);
        recStack.add(node);

        List<Long> children = graph.getOrDefault(node, new ArrayList<>());
        for (Long child : children) {
            if (hasCycle(child, graph, visited, recStack)) return true;
        }

        recStack.remove(node);
        return false;
    }

    private MetricaKpiDTO emitAndReturn(MetricaKpiDTO dto) {
        // Enviar por STOMP si hay sesión activa
        sesionRepository.findByProyectoIdAndActivaTrue(dto.proyectoId()).ifPresent(sesion -> {
            messagingTemplate.convertAndSend("/topic/sala/" + sesion.getSessionToken() + "/metricas", dto);
        });
        return dto;
    }
}
