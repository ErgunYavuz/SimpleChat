package com.chat_application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest
class ChatApplicationTests {
    private static final Logger logger = LoggerFactory.getLogger(ChatWebSocketHandler.class);


	@Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @BeforeEach
    void setUp() {
        chatWebSocketHandler.clearSessions();
    }

	@Test
	void contextLoads() {
		// This test will fail if the application context cannot start
	}


	@Test
	public void testConnexion() throws Exception{
        WebSocketSession session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn("session");

        chatWebSocketHandler.afterConnectionEstablished(session);

		assertEquals(1, chatWebSocketHandler.getSessions().size());
        assertEquals(session, chatWebSocketHandler.getSessions().get("session"));
	}

	@Test
    public void testMessageAndBroadcast() throws Exception {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);

		when(session1.getId()).thenReturn("session1");
		when(session2.getId()).thenReturn("session2");
        
        chatWebSocketHandler.afterConnectionEstablished(session1);
        chatWebSocketHandler.afterConnectionEstablished(session2);
        
        TextMessage message = new TextMessage("{\"sender\":\"user1\",\"content\":\"Hello\"}");
        chatWebSocketHandler.handleTextMessage(session1, message);
        
        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }

	
    @Test
    public void testInvalidJsonMessage() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        TextMessage invalidMessage = new TextMessage("This is not JSON");
        
        chatWebSocketHandler.handleTextMessage(session, invalidMessage);
        
        verify(session).sendMessage(argThat(message -> 
            message instanceof TextMessage &&
            ((TextMessage) message).getPayload().contains("Error: Invalid JSON format") &&
            ((TextMessage) message).getPayload().contains("Unrecognized token 'This'")
        ));
    }
	
}
