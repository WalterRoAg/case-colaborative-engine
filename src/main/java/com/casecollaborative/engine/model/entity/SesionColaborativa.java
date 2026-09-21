package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "sesion_colaborativa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionColaborativa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Column(name = "session_token", nullable = false, unique = true, length = 120)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_usuario_id", nullable = false)
    private Usuario hostUsuario;

    @Column(nullable = false)
    @Builder.Default
    private Boolean activa = true;

    @Column(name = "fecha_inicio", insertable = false, updatable = false)
    private ZonedDateTime fechaInicio;

    @Column(name = "fecha_cierre")
    private ZonedDateTime fechaCierre;
}
