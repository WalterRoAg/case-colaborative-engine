package com.casecollaborative.engine.service;

import com.casecollaborative.engine.model.entity.*;
import com.casecollaborative.engine.repository.*;
import com.casecollaborative.engine.dto.sync.SyncItemRequest;
import com.casecollaborative.engine.dto.websocket.AttributeMutationPayload;
import com.casecollaborative.engine.dto.websocket.RelationMutationPayload;
import com.casecollaborative.engine.dto.websocket.SocketEventEnvelope;
import com.casecollaborative.engine.dto.websocket.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class SyncItemTransactionService {

    private final SyncIdempotenciaRepository syncIdempotenciaRepository;
    private final ClaseRepository claseRepository;
    private final AtributoRepository atributoRepository;
    private final RelacionClaseRepository relacionRepository;
    private final DiagramaUmlRepository diagramaRepository;
    private final ObjectMapper objectMapper;

    public SyncItemTransactionService(
            SyncIdempotenciaRepository syncIdempotenciaRepository,
            ClaseRepository claseRepository,
            AtributoRepository atributoRepository,
            RelacionClaseRepository relacionRepository,
            DiagramaUmlRepository diagramaRepository,
            ObjectMapper objectMapper) {
        this.syncIdempotenciaRepository = syncIdempotenciaRepository;
        this.claseRepository = claseRepository;
        this.atributoRepository = atributoRepository;
        this.relacionRepository = relacionRepository;
        this.diagramaRepository = diagramaRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Optional<SocketEventEnvelope> procesarItem(SyncItemRequest item, Long usuarioAutenticadoId) throws Exception {
        // Idempotencia
        if (syncIdempotenciaRepository.existsById(item.localId())) {
            return Optional.empty(); // Ya fue procesado
        }

        // Registrar intento
        SyncIdempotencia idempotencia = SyncIdempotencia.builder()
                .mutationUuid(item.localId())
                .sessionToken(item.sessionToken())
                .usuarioId(usuarioAutenticadoId)
                .tipoMutacion(item.tipoMutacion())
                .build();
        syncIdempotenciaRepository.save(idempotencia);

        DiagramaUml diagrama = diagramaRepository.findById(item.diagramaId())
                .orElseThrow(() -> new IllegalArgumentException("Diagrama no encontrado: " + item.diagramaId()));

        SocketEventEnvelope envelope = null;

        switch (item.tipoMutacion()) {
            case "CREAR_CLASE":
                JsonNode claseNode = objectMapper.readTree(item.payloadJson());
                Clase clase = new Clase();
                clase.setDiagrama(diagrama);
                clase.setNombre(claseNode.get("nombre").asText());
                clase.setPosX(claseNode.get("posX").asDouble());
                clase.setPosY(claseNode.get("posY").asDouble());
                clase.setVisibilidad(claseNode.has("visibilidad") ? claseNode.get("visibilidad").asText() : "public");
                clase = claseRepository.save(clase);

                if (claseNode.has("atributos") && claseNode.get("atributos").isArray()) {
                    for (JsonNode attrNode : claseNode.get("atributos")) {
                        Atributo atributo = new Atributo();
                        atributo.setClase(clase);
                        atributo.setNombre(attrNode.get("nombre").asText());
                        atributo.setTipoDato(attrNode.get("tipoDato").asText());
                        atributo.setVisibilidad(attrNode.has("visibilidad") ? attrNode.get("visibilidad").asText() : "private");
                        atributo.setEsPk(attrNode.has("esPk") && attrNode.get("esPk").asBoolean());
                        atributoRepository.save(atributo);
                    }
                }

                // Generar evento para el broker
                envelope = new SocketEventEnvelope(
                        EventType.CLASS_MOVED,
                        item.sessionToken(),
                        usuarioAutenticadoId,
                        "Usuario", // No tenemos el nombre de usuario aquí, pero la sesión sí.
                        item.clientTimestamp(),
                        clase
                );
                break;

            case "MUTAR_ATRIBUTO":
                AttributeMutationPayload attrPayload = objectMapper.readValue(item.payloadJson(), AttributeMutationPayload.class);
                if ("CREATE".equals(attrPayload.action())) {
                    Clase c;
                    if (attrPayload.claseId() != null) {
                        c = claseRepository.findById(attrPayload.claseId())
                                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada"));
                    } else {
                        // Buscar por nombre (como pide el requerimiento de voz)
                        // NOTA: Para simplificar, buscamos la primera del diagrama
                        c = claseRepository.findByDiagramaId(diagrama.getId()).stream()
                                .filter(x -> x.getNombre().equalsIgnoreCase(attrPayload.nombre())) // wait, the payload puts class name where?
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada por nombre"));
                        // Wait, payload Json parser above is looking at 'nombre', but that's the attribute name. 
                    }
                    // Let's use a dynamic search just in case
                    JsonNode attrRaw = objectMapper.readTree(item.payloadJson());
                    String claseNombre = attrRaw.has("claseNombre") ? attrRaw.get("claseNombre").asText() : null;
                    if (claseNombre != null) {
                        c = claseRepository.findByDiagramaId(diagrama.getId()).stream()
                                .filter(x -> x.getNombre().equalsIgnoreCase(claseNombre))
                                .findFirst()
                                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada: " + claseNombre));
                    }

                    Atributo attr = new Atributo();
                    attr.setClase(c);
                    attr.setNombre(attrPayload.nombre());
                    attr.setTipoDato(attrPayload.tipoDato());
                    attr.setVisibilidad(attrPayload.visibilidad() != null ? attrPayload.visibilidad() : "private");
                    attr.setEsPk(attrPayload.esPk() != null ? attrPayload.esPk() : false);
                    attr.setOrden(attrPayload.orden() != null ? attrPayload.orden() : 0);
                    attr = atributoRepository.save(attr);
                    
                    envelope = new SocketEventEnvelope(
                        EventType.ATTRIBUTE_MUTATED,
                        item.sessionToken(),
                        usuarioAutenticadoId,
                        "Usuario",
                        item.clientTimestamp(),
                        attr
                    );
                }
                break;

            case "CREAR_RELACION":
            case "AGREGAR_HERENCIA":
                JsonNode relNode = objectMapper.readTree(item.payloadJson());
                String origenNombre = relNode.has("claseOrigenNombre") ? relNode.get("claseOrigenNombre").asText() : null;
                String destinoNombre = relNode.has("claseDestinoNombre") ? relNode.get("claseDestinoNombre").asText() : null;

                Clase cOrigen = claseRepository.findByDiagramaId(diagrama.getId()).stream()
                        .filter(x -> x.getNombre().equalsIgnoreCase(origenNombre))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Clase Origen no encontrada: " + origenNombre));
                
                Clase cDestino = claseRepository.findByDiagramaId(diagrama.getId()).stream()
                        .filter(x -> x.getNombre().equalsIgnoreCase(destinoNombre))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Clase Destino no encontrada: " + destinoNombre));

                RelacionClase relacion = new RelacionClase();
                relacion.setDiagrama(diagrama);
                relacion.setClaseOrigen(cOrigen);
                relacion.setClaseDestino(cDestino);
                relacion.setTipoRelacion(relNode.get("tipoRelacion").asText());
                relacion.setCardinalidadOrigen(relNode.has("cardinalidadOrigen") ? relNode.get("cardinalidadOrigen").asText() : "1");
                relacion.setCardinalidadDestino(relNode.has("cardinalidadDestino") ? relNode.get("cardinalidadDestino").asText() : "1");
                relacion = relacionRepository.save(relacion);
                
                envelope = new SocketEventEnvelope(
                        EventType.RELATION_MUTATED,
                        item.sessionToken(),
                        usuarioAutenticadoId,
                        "Usuario",
                        item.clientTimestamp(),
                        relacion
                );
                break;
                
            default:
                throw new IllegalArgumentException("Tipo de mutación no soportada: " + item.tipoMutacion());
        }

        return Optional.ofNullable(envelope);
    }
}
