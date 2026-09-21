package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "atributo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Atributo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clase_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Clase clase;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_dato", nullable = false, length = 50)
    private String tipoDato;

    @Column(length = 20)
    @Builder.Default
    private String visibilidad = "private";

    @Column(name = "es_pk")
    @Builder.Default
    private Boolean esPk = false;

    @Builder.Default
    private Integer orden = 0;
}
