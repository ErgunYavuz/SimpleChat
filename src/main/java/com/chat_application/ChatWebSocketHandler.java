package com.chat_application;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.lang.NonNull;
import org.slf4j.LoggerFactory;

import org.slf4j.Logger;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final Map<String, WebSocketSession> sessions  = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        logger.info("New WebSocket connection established: {}", session.getId());
        logger.info(sessions.toString());

    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        sessions.values().remove(session);
        logger.info("WebSocket connection closed: {}", session.getId());
    }

    @Override
    public void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        try{
            ChatMessage chatMessage = objectMapper.readValue(message.getPayload(), ChatMessage.class);
            chatMessage.setTimestamp(System.currentTimeMillis());
            logger.info("Received message from {}: {}", session.getId(), chatMessage.toString());

            messageRepository.save(chatMessage);
    
            //broadcast to all clients
            TextMessage outMessage = new TextMessage(objectMapper.writeValueAsString(chatMessage));
            for (WebSocketSession s : sessions.values()) {
                s.sendMessage(outMessage);
            }
            logger.info("Message broadcasted to {} clients", sessions.size());
    
        } catch (com.fasterxml.jackson.core.JsonParseException e){
            String errorMessage = "Error: Invalid JSON format - " + e.getMessage();
            session.sendMessage(new TextMessage(errorMessage));
        }
       
    }

    public Map<String, WebSocketSession> getSessions(){
        return sessions;
    }
    
    public void clearSessions() {
        sessions.clear();
    }
}