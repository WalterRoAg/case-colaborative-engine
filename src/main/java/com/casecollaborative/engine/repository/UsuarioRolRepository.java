package com.casecollaborative.engine.repository;

import com.casecollaborative.engine.model.entity.UsuarioRol;
import com.casecollaborative.engine.model.entity.UsuarioRolId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRolRepository extends JpaRepository<UsuarioRol, UsuarioRolId> {
}
