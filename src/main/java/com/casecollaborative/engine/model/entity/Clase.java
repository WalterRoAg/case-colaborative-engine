package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "clase")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Clase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diagrama_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DiagramaUml diagrama;

    @OneToMany(mappedBy = "clase", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @Builder.Default
    private java.util.List<Atributo> atributos = new java.util.ArrayList<>();

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 50)
    @Builder.Default
    private String estereotipo = "CLASS";

    @Column(length = 20)
    @Builder.Default
    private String visibilidad = "public";

    @Column(name = "pos_x")
    @Builder.Default
    private Double posX = 0.0;

    @Column(name = "pos_y")
    @Builder.Default
    private Double posY = 0.0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bloqueado_por_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Usuario bloqueadoPor;

    @Column(name = "bloqueado_en")
    private ZonedDateTime bloqueadoEn;
}
