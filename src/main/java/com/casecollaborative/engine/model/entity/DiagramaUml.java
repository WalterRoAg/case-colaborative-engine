package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "diagrama_uml")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagramaUml {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false, unique = true)
    private Proyecto proyecto;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Builder.Default
    private Integer version = 1;

    @Column(name = "zoom_canvas")
    @Builder.Default
    private Double zoomCanvas = 1.0;

    @Column(name = "pan_x")
    @Builder.Default
    private Double panX = 0.0;

    @Column(name = "pan_y")
    @Builder.Default
    private Double panY = 0.0;

    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private ZonedDateTime fechaActualizacion;
}
