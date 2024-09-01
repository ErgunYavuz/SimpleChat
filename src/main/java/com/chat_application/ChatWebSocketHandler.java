package com.chat_application;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // Connection established, but username not set yet
        System.out.println("New WebSocket connection established: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            JsonNode jsonNode = objectMapper.readTree(message.getPayload());
            
            if (jsonNode.has("type")) {
                String messageType = jsonNode.get("type").asText();
                switch (messageType) {
                    case "LOGIN":
                        handleLoginMessage(session, jsonNode);
                        break;
                    case "CHAT":
                        handleChatMessage(session, jsonNode);
                        break;
                    default:
                        System.out.println("Unrecognized message type: " + messageType);
                }
            } else {
                System.out.println("Received message without type: " + message.getPayload());
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            sendErrorMessage(session, "Error processing message");
        }
    }

    private void handleLoginMessage(WebSocketSession session, JsonNode jsonNode) throws IOException {
        String username = jsonNode.get("sender").asText();

        if (!isValidUsername(username)) {
            sendErrorMessage(session, "Invalid username. Use 3-20 characters, only letters, numbers, and underscores.");
            return;
        }

        if (sessions.containsKey(username)) {
            sendErrorMessage(session, "Username already taken. Please choose another.");
            return;
        }

        // Username is valid and available
        sessions.put(username, session);
        session.getAttributes().put("username", username);
        
        // Send welcome message
        sendMessage(session, "System", "Welcome to the chat, " + username + "!");
        broadcastMessage("System", username + " has joined the chat");


        // Broadcast updated user list
        broadcastUserList();
    }

    private void handleChatMessage(WebSocketSession session, JsonNode jsonNode) throws IOException {
        String sender = (String) session.getAttributes().get("username");
        if (sender == null) {
            sendErrorMessage(session, "You must be logged in to send messages.");
            return;
        }

        String content = jsonNode.get("content").asText();
        broadcastMessage(sender, content);
    }

    private boolean isValidUsername(String username) {
        return USERNAME_PATTERN.matcher(username).matches();
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage) throws IOException {
        ObjectNode errorNode = objectMapper.createObjectNode();
        errorNode.put("type", "ERROR");
        errorNode.put("message", errorMessage);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(errorNode)));
    }

    private void sendMessage(WebSocketSession session, String sender, String content) throws IOException {
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("type", "CHAT");
        messageNode.put("sender", sender);
        messageNode.put("content", content);
        messageNode.put("timestamp", System.currentTimeMillis());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(messageNode)));
    }

    private void broadcastMessage(String sender, String content) throws IOException {
        ObjectNode messageNode = objectMapper.createObjectNode();
        messageNode.put("type", "CHAT");
        messageNode.put("sender", sender);
        messageNode.put("content", content);
        messageNode.put("timestamp", System.currentTimeMillis());
        
        String messageJson = objectMapper.writeValueAsString(messageNode);
        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(new TextMessage(messageJson));
        }
    }

    private void broadcastUserList() throws IOException {
        Set<String> usernames = sessions.keySet();
        
        ObjectNode userListNode = objectMapper.createObjectNode();
        userListNode.put("type", "USER_LIST");
        userListNode.set("users", objectMapper.valueToTree(usernames));
        
        String userListJson = objectMapper.writeValueAsString(userListNode);
        for (WebSocketSession session : sessions.values()) {
            session.sendMessage(new TextMessage(userListJson));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String username = (String) session.getAttributes().get("username");
        if (username != null) {
            sessions.remove(username);
            broadcastUserList();
            broadcastMessage("System", username + " has left the chat.");
        }
        System.out.println("WebSocket connection closed: " + session.getId());
    }

    protected String getConnectedUsers(){
        return sessions.keySet().toString();
    }
    
    public Map<String, WebSocketSession> getSessions(){
        return sessions;
    }
    
    
    public void clearSessions() {
        sessions.clear();
    }
    
}