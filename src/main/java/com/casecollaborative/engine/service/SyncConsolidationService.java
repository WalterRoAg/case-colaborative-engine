package com.casecollaborative.engine.service;

import com.casecollaborative.engine.dto.sync.*;
import com.casecollaborative.engine.dto.websocket.SocketEventEnvelope;
import com.casecollaborative.engine.model.entity.Proyecto;
import com.casecollaborative.engine.model.entity.SesionColaborativa;
import com.casecollaborative.engine.repository.ProyectoColaboradorRepository;
import com.casecollaborative.engine.repository.ProyectoRepository;
import com.casecollaborative.engine.repository.SesionColaborativaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SyncConsolidationService {

    private static final Logger log = LoggerFactory.getLogger(SyncConsolidationService.class);

    private final SyncItemTransactionService syncItemTransactionService;
    private final ProyectoRepository proyectoRepository;
    private final ProyectoColaboradorRepository proyectoColaboradorRepository;
    private final SesionColaborativaRepository sesionColaborativaRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public SyncConsolidationService(SyncItemTransactionService syncItemTransactionService,
                                    ProyectoRepository proyectoRepository,
                                    ProyectoColaboradorRepository proyectoColaboradorRepository,
                                    SesionColaborativaRepository sesionColaborativaRepository,
                                    SimpMessagingTemplate messagingTemplate) {
        this.syncItemTransactionService = syncItemTransactionService;
        this.proyectoRepository = proyectoRepository;
        this.proyectoColaboradorRepository = proyectoColaboradorRepository;
        this.sesionColaborativaRepository = sesionColaborativaRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public BatchSyncResponse procesarLote(BatchSyncRequest request, Long usuarioId, String nombreUsuario) {
        int total = request.mutaciones().size();
        int exitosos = 0;
        int fallidos = 0;
        List<SyncItemResult> resultados = new ArrayList<>();

        for (SyncItemRequest item : request.mutaciones()) {
            try {
                // 1. Validaciones
                validarAcceso(item, usuarioId);

                // 2. Procesar atómicamente con REQUIRES_NEW
                Optional<SocketEventEnvelope> optEnvelope = syncItemTransactionService.procesarItem(item, usuarioId);
                
                // 3. Post-commit (ya que procesarItem completó sin excepciones)
                optEnvelope.ifPresent(envelope -> {
                    messagingTemplate.convertAndSend("/topic/sala/" + item.sessionToken() + "/canvas", envelope);
                });

                resultados.add(new SyncItemResult(item.localId(), true, "OK"));
                exitosos++;
            } catch (Exception e) {
                log.error("Error procesando mutación offline {}: {}", item.localId(), e.getMessage());
                resultados.add(new SyncItemResult(item.localId(), false, e.getMessage()));
                fallidos++;
            }
        }

        return new BatchSyncResponse(total, exitosos, fallidos, resultados, System.currentTimeMillis());
    }

    private void validarAcceso(SyncItemRequest item, Long usuarioId) {
        Proyecto p = proyectoRepository.findById(item.proyectoId())
                .orElseThrow(() -> new IllegalArgumentException("Proyecto no existe"));

        if (!p.getPropietario().getId().equals(usuarioId) &&
            !proyectoColaboradorRepository.existsByProyectoIdAndColaboradorId(item.proyectoId(), usuarioId)) {
            throw new SecurityException("Usuario no autorizado en el proyecto");
        }

        SesionColaborativa sc = sesionColaborativaRepository.findBySessionToken(item.sessionToken())
                .orElseThrow(() -> new IllegalArgumentException("Token de sesión no existe"));

        if (!sc.getActiva() || !sc.getProyecto().getId().equals(item.proyectoId())) {
            throw new SecurityException("Sesión inválida o inactiva para este proyecto");
        }
    }
}
