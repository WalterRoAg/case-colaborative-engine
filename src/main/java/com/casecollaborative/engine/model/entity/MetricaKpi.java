package com.casecollaborative.engine.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "metrica_kpi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricaKpi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Column(name = "tiempo_modelado_seg")
    @Builder.Default
    private Integer tiempoModeladoSeg = 0;

    @Column(name = "elementos_por_minuto", precision = 6, scale = 2)
    @Builder.Default
    private BigDecimal elementosPorMinuto = BigDecimal.ZERO;

    @Column(name = "acoplamiento_cbo", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal acoplamientoCbo = BigDecimal.ZERO;

    @Column(name = "violaciones_uml")
    @Builder.Default
    private Integer violacionesUml = 0;

    @Column(name = "tasa_compilacion", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal tasaCompilacion = BigDecimal.ZERO;
}
