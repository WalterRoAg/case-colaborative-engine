package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.SyncIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncIdempotenciaRepository extends JpaRepository<SyncIdempotencia, String> {
}
