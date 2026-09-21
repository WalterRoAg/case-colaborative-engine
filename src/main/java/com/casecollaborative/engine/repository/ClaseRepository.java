package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.Clase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClaseRepository extends JpaRepository<Clase, Long> {
    List<Clase> findByDiagramaId(Long diagramaId);
}
