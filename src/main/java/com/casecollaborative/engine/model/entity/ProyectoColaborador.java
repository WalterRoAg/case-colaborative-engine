package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "proyecto_colaborador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProyectoColaborador {
    @EmbeddedId
    private ProyectoColaboradorId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("proyectoId")
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("colaboradorId")
    @JoinColumn(name = "colaborador_id")
    private Usuario colaborador;

    @Column(name = "fecha_asignacion", insertable = false, updatable = false)
    private ZonedDateTime fechaAsignacion;
}
