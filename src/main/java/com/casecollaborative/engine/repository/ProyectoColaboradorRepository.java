package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.ProyectoColaborador;
import com.casecollaborative.engine.model.entity.ProyectoColaboradorId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProyectoColaboradorRepository extends JpaRepository<ProyectoColaborador, ProyectoColaboradorId> {
    boolean existsByProyectoIdAndColaboradorId(Long proyectoId, Long colaboradorId);
}
