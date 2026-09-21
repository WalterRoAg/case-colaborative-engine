package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.SesionColaborativa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SesionColaborativaRepository extends JpaRepository<SesionColaborativa, Long> {
    Optional<SesionColaborativa> findBySessionToken(String sessionToken);

    Optional<SesionColaborativa> findBySessionTokenAndActivaTrue(String sessionToken);
    
    List<SesionColaborativa> findAllByProyectoIdAndActivaTrueOrderByIdDesc(Long proyectoId);

    default Optional<SesionColaborativa> findByProyectoIdAndActivaTrue(Long proyectoId) {
        List<SesionColaborativa> list = findAllByProyectoIdAndActivaTrueOrderByIdDesc(proyectoId);
        return (list != null && !list.isEmpty()) ? Optional.of(list.get(0)) : Optional.empty();
    }
    
    List<SesionColaborativa> findAllByProyectoIdAndActivaTrue(Long proyectoId);
}

