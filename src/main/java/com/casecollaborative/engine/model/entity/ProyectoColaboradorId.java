package com.casecollaborative.engine.model.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProyectoColaboradorId implements Serializable {
    private Long proyectoId;
    private Long colaboradorId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProyectoColaboradorId that = (ProyectoColaboradorId) o;
        return Objects.equals(proyectoId, that.proyectoId) &&
                Objects.equals(colaboradorId, that.colaboradorId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proyectoId, colaboradorId);
    }
}
