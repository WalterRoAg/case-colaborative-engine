package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.MetricaKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricaKpiRepository extends JpaRepository<MetricaKpi, Long> {
    List<MetricaKpi> findAllByProyectoIdOrderByIdDesc(Long proyectoId);

    default Optional<MetricaKpi> findByProyectoId(Long proyectoId) {
        List<MetricaKpi> list = findAllByProyectoIdOrderByIdDesc(proyectoId);
        return (list != null && !list.isEmpty()) ? Optional.of(list.get(0)) : Optional.empty();
    }
}

