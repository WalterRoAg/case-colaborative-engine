package com.casecollaborative.engine.controller;

import com.casecollaborative.engine.dto.websocket.*;
import com.casecollaborative.engine.repository.AtributoRepository;
import com.casecollaborative.engine.repository.ClaseRepository;
import com.casecollaborative.engine.repository.RelacionClaseRepository;
import com.casecollaborative.engine.security.CustomUserDetails;
import com.casecollaborative.engine.service.CoordinadorBloqueoPesimista;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

@Controller
public class LienzoSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final CoordinadorBloqueoPesimista coordinadorBloqueoPesimista;
    private final ClaseRepository claseRepository;
    private final AtributoRepository atributoRepository;
    private final RelacionClaseRepository relacionRepository;
    private final com.casecollaborative.engine.repository.SesionColaborativaRepository sesionRepository;
    private final com.casecollaborative.engine.repository.DiagramaUmlRepository diagramaRepository;

    public LienzoSocketController(SimpMessageSendingOperations messagingTemplate,
                                  CoordinadorBloqueoPesimista coordinadorBloqueoPesimista,
                                  ClaseRepository claseRepository,
                                  AtributoRepository atributoRepository,
                                  RelacionClaseRepository relacionRepository,
                                  com.casecollaborative.engine.repository.SesionColaborativaRepository sesionRepository,
                                  com.casecollaborative.engine.repository.DiagramaUmlRepository diagramaRepository) {
        this.messagingTemplate = messagingTemplate;
        this.coordinadorBloqueoPesimista = coordinadorBloqueoPesimista;
        this.claseRepository = claseRepository;
        this.atributoRepository = atributoRepository;
        this.relacionRepository = relacionRepository;
        this.sesionRepository = sesionRepository;
        this.diagramaRepository = diagramaRepository;
    }

    private CustomUserDetails getUser(SimpMessageHeaderAccessor headerAccessor) {
        UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails ud) {
            return ud;
        }
        return null;
    }

    @MessageMapping("/sala/{sessionToken}/presencia/unirse")
    public void usuarioSeUnio(@DestinationVariable String sessionToken, @Payload(required = false) java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("usuarioId", user.getId());
            data.put("nombre", user.getUsername());
            data.put("rol", (payload != null && payload.get("rol") != null) ? payload.get("rol") : "COLABORADOR");

            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.USER_JOINED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), data
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }

    @MessageMapping("/sala/{sessionToken}/presencia/cursor")
    public void cursorMoved(@DestinationVariable String sessionToken, @Payload CursorPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.CURSOR_MOVED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }

    @MessageMapping("/sala/{sessionToken}/lock/solicitar")
    public void solicitarLock(@DestinationVariable String sessionToken, @Payload LockPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            boolean acquired = coordinadorBloqueoPesimista.intentarAdquirirLock(payload.elementId(), user.getId(), user.getUsername());
            
            LockPayload responsePayload;
            if (acquired) {
                responsePayload = new LockPayload(payload.elementId(), payload.elementType(), user.getId(), user.getUsername(), true);
                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.LOCK_GRANTED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), responsePayload
                );
                messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
                messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/reply", envelope);
            } else {
                var currentLock = coordinadorBloqueoPesimista.getLock(payload.elementId());
                String lockedByName = currentLock != null ? currentLock.usuarioNombre() : "Unknown";
                Long lockedById = currentLock != null ? currentLock.usuarioId() : -1L;
                
                responsePayload = new LockPayload(payload.elementId(), payload.elementType(), lockedById, lockedByName, false);
                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.LOCK_DENIED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), responsePayload
                );
                messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/reply", envelope);
            }
        }
    }

    @MessageMapping("/sala/{sessionToken}/lock/liberar")
    public void liberarLock(@DestinationVariable String sessionToken, @Payload LockPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            boolean released = coordinadorBloqueoPesimista.liberarLock(payload.elementId(), user.getId());
            if (released) {
                LockPayload responsePayload = new LockPayload(payload.elementId(), payload.elementType(), user.getId(), user.getUsername(), false);
                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.LOCK_RELEASED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), responsePayload
                );
                messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
            }
        }
    }

    @MessageMapping("/sala/{sessionToken}/clase/mover")
    public void moverClase(@DestinationVariable String sessionToken, @Payload ClassMovePayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null && payload != null && payload.claseId() != null) {
            long ts = payload.clientTimestamp() != null ? payload.clientTimestamp() : System.currentTimeMillis();
            if (coordinadorBloqueoPesimista.validarLWW(payload.claseId(), ts)) {
                claseRepository.findById(payload.claseId()).ifPresent(clase -> {
                    clase.setPosX(payload.posX());
                    clase.setPosY(payload.posY());
                    claseRepository.save(clase);
                });
                
                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.CLASS_MOVED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
                );
                messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
            }
        }
    }

    // Additional methods for ATTRIBUTE_MUTATED and RELATION_MUTATED would follow a similar pattern
    @MessageMapping("/sala/{sessionToken}/atributo/mutar")
    public void mutarAtributo(@DestinationVariable String sessionToken, @Payload AttributeMutationPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            AttributeMutationPayload resultPayload = payload;
            try {
                if ("DELETE".equalsIgnoreCase(payload.action())) {
                    if (payload.atributoId() != null) {
                        atributoRepository.deleteById(payload.atributoId());
                    }
                } else if ("UPDATE".equalsIgnoreCase(payload.action())) {
                    if (payload.atributoId() != null) {
                        var attrOpt = atributoRepository.findById(payload.atributoId());
                        if (attrOpt.isPresent()) {
                            var attr = attrOpt.get();
                            if (payload.nombre() != null) attr.setNombre(payload.nombre());
                            if (payload.tipoDato() != null) attr.setTipoDato(payload.tipoDato());
                            if (payload.visibilidad() != null) attr.setVisibilidad(payload.visibilidad());
                            if (payload.esPk() != null) attr.setEsPk(payload.esPk());
                            atributoRepository.save(attr);
                        }
                    }
                } else {
                    // CREATE action
                    if (payload.claseId() != null) {
                        var claseOpt = claseRepository.findById(payload.claseId());
                        if (claseOpt.isPresent()) {
                            var clase = claseOpt.get();
                            com.casecollaborative.engine.model.entity.Atributo attr = new com.casecollaborative.engine.model.entity.Atributo();
                            attr.setClase(clase);
                            attr.setNombre(payload.nombre() != null ? payload.nombre() : "nuevoAtributo");
                            attr.setTipoDato(payload.tipoDato() != null ? payload.tipoDato() : "String");
                            attr.setVisibilidad(payload.visibilidad() != null ? payload.visibilidad() : "private");
                            attr.setEsPk(Boolean.TRUE.equals(payload.esPk()));
                            attr.setOrden(payload.orden() != null ? payload.orden() : 0);
                            attr = atributoRepository.save(attr);

                            resultPayload = new AttributeMutationPayload(
                                    attr.getId(), clase.getId(), attr.getNombre(), attr.getTipoDato(),
                                    attr.getVisibilidad(), attr.getEsPk(), attr.getOrden(), "CREATE"
                            );
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.ATTRIBUTE_MUTATED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), resultPayload
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }

    @MessageMapping("/sala/{sessionToken}/relacion/mutar")
    public void mutarRelacion(@DestinationVariable String sessionToken, @Payload RelationMutationPayload payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            try {
                if ("DELETE".equalsIgnoreCase(payload.action())) {
                    if (payload.relacionId() != null) {
                        relacionRepository.deleteById(payload.relacionId());
                    }
                } else if ("UPDATE".equalsIgnoreCase(payload.action())) {
                    if (payload.relacionId() != null) {
                        var relOpt = relacionRepository.findById(payload.relacionId());
                        if (relOpt.isPresent()) {
                            var rel = relOpt.get();
                            if (payload.tipoRelacion() != null) rel.setTipoRelacion(payload.tipoRelacion());
                            if (payload.nombre() != null) rel.setNombre(payload.nombre());
                            if (payload.cardinalidadOrigen() != null) rel.setCardinalidadOrigen(payload.cardinalidadOrigen());
                            if (payload.cardinalidadDestino() != null) rel.setCardinalidadDestino(payload.cardinalidadDestino());
                            relacionRepository.save(rel);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.RELATION_MUTATED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }

    @MessageMapping("/sala/{sessionToken}/clase/agregar")
    public void agregarClase(@DestinationVariable String sessionToken, @Payload java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            sesionRepository.findBySessionToken(sessionToken).ifPresent(sesion -> {
                var diagrama = diagramaRepository.findByProyectoId(sesion.getProyecto().getId())
                        .orElseGet(() -> {
                            com.casecollaborative.engine.model.entity.DiagramaUml d = new com.casecollaborative.engine.model.entity.DiagramaUml();
                            d.setProyecto(sesion.getProyecto());
                            d.setNombre(sesion.getProyecto().getNombre());
                            d.setVersion(1);
                            d.setZoomCanvas(1.0);
                            d.setPanX(0.0);
                            d.setPanY(0.0);
                            return diagramaRepository.save(d);
                        });

                com.casecollaborative.engine.model.entity.Clase clase = new com.casecollaborative.engine.model.entity.Clase();
                clase.setDiagrama(diagrama);
                clase.setNombre(payload.get("nombre") != null ? payload.get("nombre").toString() : "NuevaClase");
                clase.setEstereotipo(payload.get("estereotipo") != null ? payload.get("estereotipo").toString() : "CLASS");
                clase.setPosX(payload.get("posX") != null ? Double.parseDouble(payload.get("posX").toString()) : 120.0);
                clase.setPosY(payload.get("posY") != null ? Double.parseDouble(payload.get("posY").toString()) : 120.0);
                clase.setVisibilidad("public");
                clase = claseRepository.save(clase);

                // Atributos iniciales si existen
                if (payload.get("atributos") instanceof java.util.List<?> listaAtributos) {
                    for (Object o : listaAtributos) {
                        if (o instanceof java.util.Map<?, ?> attrMap) {
                            com.casecollaborative.engine.model.entity.Atributo attr = new com.casecollaborative.engine.model.entity.Atributo();
                            attr.setClase(clase);
                            attr.setNombre(attrMap.get("nombre") != null ? attrMap.get("nombre").toString() : "id");
                            attr.setTipoDato(attrMap.get("tipo") != null ? attrMap.get("tipo").toString() : "Long");
                            attr.setVisibilidad("private");
                            attr.setEsPk(Boolean.TRUE.equals(attrMap.get("esPk")));
                            attr.setOrden(0);
                            atributoRepository.save(attr);
                        }
                    }
                }

                payload.put("id", clase.getId());
                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.CLASS_CREATED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
                );
                messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
            });
        }
    }

    @MessageMapping("/sala/{sessionToken}/clase/modificar")
    public void modificarClase(@DestinationVariable String sessionToken, @Payload java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null && payload.get("claseId") != null) {
            Long claseId = Long.parseLong(payload.get("claseId").toString());
            claseRepository.findById(claseId).ifPresent(clase -> {
                if (payload.get("nombre") != null) clase.setNombre(payload.get("nombre").toString());
                if (payload.get("estereotipo") != null) clase.setEstereotipo(payload.get("estereotipo").toString());
                if (payload.get("visibilidad") != null) clase.setVisibilidad(payload.get("visibilidad").toString());
                claseRepository.save(clase);

                SocketEventEnvelope envelope = new SocketEventEnvelope(
                        EventType.CLASS_MOVED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
                );
                messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
            });
        }
    }

    @MessageMapping("/sala/{sessionToken}/clase/eliminar")
    public void eliminarClase(@DestinationVariable String sessionToken, @Payload java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null && payload.get("claseId") != null) {
            Long claseId = Long.parseLong(payload.get("claseId").toString());
            // Eliminar relaciones vinculadas
            relacionRepository.deleteByClaseOrigenIdOrClaseDestinoId(claseId, claseId);
            // Eliminar clase
            claseRepository.deleteById(claseId);

            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.CLASS_DELETED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }

    @MessageMapping("/sala/{sessionToken}/relacion/agregar")
    public void agregarRelacion(@DestinationVariable String sessionToken, @Payload java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null) {
            sesionRepository.findBySessionToken(sessionToken).ifPresent(sesion -> {
                var diagrama = diagramaRepository.findByProyectoId(sesion.getProyecto().getId())
                        .orElseGet(() -> {
                            com.casecollaborative.engine.model.entity.DiagramaUml d = new com.casecollaborative.engine.model.entity.DiagramaUml();
                            d.setProyecto(sesion.getProyecto());
                            d.setNombre(sesion.getProyecto().getNombre());
                            d.setVersion(1);
                            d.setZoomCanvas(1.0);
                            d.setPanX(0.0);
                            d.setPanY(0.0);
                            return diagramaRepository.save(d);
                        });

                Long origenId = Long.parseLong(payload.get("origenId").toString());
                Long destinoId = Long.parseLong(payload.get("destinoId").toString());
                String tipo = payload.get("tipo").toString();

                claseRepository.findById(origenId).ifPresent(origen -> {
                    claseRepository.findById(destinoId).ifPresent(destino -> {
                        com.casecollaborative.engine.model.entity.RelacionClase relacion = new com.casecollaborative.engine.model.entity.RelacionClase();
                        relacion.setDiagrama(diagrama);
                        relacion.setClaseOrigen(origen);
                        relacion.setClaseDestino(destino);
                        relacion.setTipoRelacion(tipo);
                        relacion.setCardinalidadOrigen(payload.get("cardinalidadOrigen") != null ? payload.get("cardinalidadOrigen").toString() : "1");
                        relacion.setCardinalidadDestino(payload.get("cardinalidadDestino") != null ? payload.get("cardinalidadDestino").toString() : "1");
                        if (payload.get("nombre") != null) relacion.setNombre(payload.get("nombre").toString());
                        relacion = relacionRepository.save(relacion);

                        // Broadcast
                        payload.put("id", relacion.getId());
                        SocketEventEnvelope envelope = new SocketEventEnvelope(
                                EventType.RELATION_MUTATED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
                        );
                        messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
                    });
                });
            });
        }
    }

    @MessageMapping("/sala/{sessionToken}/relacion/eliminar")
    public void eliminarRelacion(@DestinationVariable String sessionToken, @Payload java.util.Map<String, Object> payload, SimpMessageHeaderAccessor headerAccessor) {
        CustomUserDetails user = getUser(headerAccessor);
        if (user != null && payload.get("relacionId") != null) {
            Long relId = Long.parseLong(payload.get("relacionId").toString());
            relacionRepository.deleteById(relId);

            SocketEventEnvelope envelope = new SocketEventEnvelope(
                    EventType.RELATION_MUTATED, sessionToken, user.getId(), user.getUsername(), System.currentTimeMillis(), payload
            );
            messagingTemplate.convertAndSend("/topic/sala/" + sessionToken, envelope);
        }
    }
}
