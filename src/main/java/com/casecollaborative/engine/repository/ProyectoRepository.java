package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.Proyecto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProyectoRepository extends JpaRepository<Proyecto, Long> {
    List<Proyecto> findByPropietarioId(Long propietarioId);
}
