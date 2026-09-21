package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "relacion_clase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelacionClase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagrama_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DiagramaUml diagrama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_origen_id", nullable = false)
    private Clase claseOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_destino_id", nullable = false)
    private Clase claseDestino;

    @Column(name = "tipo_relacion", nullable = false, length = 40)
    private String tipoRelacion;

    @Column(length = 100)
    private String nombre;

    @Column(name = "cardinalidad_origen", length = 10)
    @Builder.Default
    private String cardinalidadOrigen = "1";

    @Column(name = "cardinalidad_destino", length = 10)
    @Builder.Default
    private String cardinalidadDestino = "1";
}
