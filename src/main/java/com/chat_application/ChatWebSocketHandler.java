package com.chat_application;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.CloseStatus;
import org.springframework.lang.NonNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler() {
    this.objectMapper = new ObjectMapper();
    this.objectMapper.registerModule(new JavaTimeModule());
}

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // Connection established, but username not set yet
        logger.info("New WebSocket connection established: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws IOException {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            
            if (jsonNode.has("type") && "LOGIN".equals(jsonNode.get("type").asText())) {
                // Handle login
                String username = jsonNode.get("sender").asText();
                sessions.put(username, session);
                broadcastMessage(new ChatMessage("System", username + " has joined the chat", null));
            } else {
                // Handle regular chat message
                String sender = getSenderForSession(session);
                if (sender != null) {
                    ChatMessage chatMessage = objectMapper.treeToValue(jsonNode, ChatMessage.class);
                    chatMessage.setSender(sender);
                    broadcastMessage(chatMessage);
                } else {
                    session.sendMessage(new TextMessage("Error: Please login first"));
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            session.sendMessage(new TextMessage("Error processing message"));
        }
    }

    private String getSenderForSession(WebSocketSession session) {
        return sessions.entrySet()
                       .stream()
                       .filter(entry -> entry.getValue().equals(session))
                       .map(Map.Entry::getKey)
                       .findFirst()
                       .orElse(null);
    }

    private void broadcastMessage(ChatMessage message) throws IOException {
        TextMessage textMessage = new TextMessage(objectMapper.writeValueAsString(message));
        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(textMessage);
        }
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String username = getSenderForSession(session);
        if (username != null) {
            sessions.remove(username);
            broadcastMessage(new ChatMessage("System", username + " has left the chat", null));
        }
        logger.info("WebSocket connection closed: " + session.getId());
    }

    public Map<String, WebSocketSession> getSessions(){
        return sessions;
    }
    
    public void clearSessions() {
        sessions.clear();
    }
}