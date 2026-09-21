package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.Atributo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AtributoRepository extends JpaRepository<Atributo, Long> {
    List<Atributo> findByClaseId(Long claseId);
}
