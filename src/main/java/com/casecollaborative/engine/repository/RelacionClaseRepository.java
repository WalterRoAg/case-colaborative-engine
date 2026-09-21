package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.RelacionClase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RelacionClaseRepository extends JpaRepository<RelacionClase, Long> {
    List<RelacionClase> findByDiagramaId(Long diagramaId);
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByClaseOrigenIdOrClaseDestinoId(Long origenId, Long destinoId);
}
