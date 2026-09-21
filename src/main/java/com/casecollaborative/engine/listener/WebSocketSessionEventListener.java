package com.casecollaborative.engine.listener;

import com.casecollaborative.engine.dto.websocket.EventType;
import com.casecollaborative.engine.dto.websocket.SocketEventEnvelope;
import com.casecollaborative.engine.security.CustomUserDetails;
import com.casecollaborative.engine.service.CoordinadorBloqueoPesimista;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketSessionEventListener {

    private final SimpMessageSendingOperations messagingTemplate;
    private final CoordinadorBloqueoPesimista coordinadorBloqueoPesimista;

    // session_id -> { room_token, user_id, user_name }
    private final Map<String, SessionInfo> sessionMap = new ConcurrentHashMap<>();

    public WebSocketSessionEventListener(SimpMessageSendingOperations messagingTemplate,
                                         CoordinadorBloqueoPesimista coordinadorBloqueoPesimista) {
        this.messagingTemplate = messagingTemplate;
        this.coordinadorBloqueoPesimista = coordinadorBloqueoPesimista;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        // Log connection if needed
    }

    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        
        if (destination != null && destination.startsWith("/topic/sala/")) {
            String roomToken = destination.substring("/topic/sala/".length());
            
            UsernamePasswordAuthenticationToken auth = (UsernamePasswordAuthenticationToken) headerAccessor.getUser();
            if (auth != null && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                Long userId = userDetails.getId();
                String userName = userDetails.getUsername(); // Correo or Name

                sessionMap.put(sessionId, new SessionInfo(roomToken, userId, userName));

                SocketEventEnvelope joinEvent = new SocketEventEnvelope(
                        EventType.USER_JOINED,
                        roomToken,
                        userId,
                        userName,
                        System.currentTimeMillis(),
                        null
                );
                
                messagingTemplate.convertAndSend(destination, joinEvent);
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        SessionInfo sessionInfo = sessionMap.remove(sessionId);

        if (sessionInfo != null) {
            coordinadorBloqueoPesimista.liberarTodosLocksDeUsuario(sessionInfo.userId());

            SocketEventEnvelope leaveEvent = new SocketEventEnvelope(
                    EventType.USER_LEFT,
                    sessionInfo.roomToken(),
                    sessionInfo.userId(),
                    sessionInfo.userName(),
                    System.currentTimeMillis(),
                    null
            );

            messagingTemplate.convertAndSend("/topic/sala/" + sessionInfo.roomToken(), leaveEvent);
        }
    }

    record SessionInfo(String roomToken, Long userId, String userName) {}
}
