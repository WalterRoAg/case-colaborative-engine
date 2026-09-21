package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "sync_idempotencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncIdempotencia {
    @Id
    @Column(name = "mutation_uuid", length = 60)
    private String mutationUuid;

    @Column(name = "session_token", length = 120, nullable = false)
    private String sessionToken;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "tipo_mutacion", length = 40, nullable = false)
    private String tipoMutacion;

    @Column(name = "procesado_en")
    @Builder.Default
    private ZonedDateTime procesadoEn = ZonedDateTime.now();
}
