package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.DiagramaUml;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiagramaUmlRepository extends JpaRepository<DiagramaUml, Long> {
    
    List<DiagramaUml> findAllByProyectoId(Long proyectoId);

    List<DiagramaUml> findByProyectoIdOrderByIdAsc(Long proyectoId);

    default Optional<DiagramaUml> findByProyectoId(Long proyectoId) {
        List<DiagramaUml> list = findByProyectoIdOrderByIdAsc(proyectoId);
        return (list != null && !list.isEmpty()) ? Optional.of(list.get(0)) : Optional.empty();
    }
}

