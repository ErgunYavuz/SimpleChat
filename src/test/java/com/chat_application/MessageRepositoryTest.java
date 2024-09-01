package com.chat_application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class MessageRepositoryTest {

    @Mock
    private MessageRepository messageRepository;

    @Test
    void testSaveMessage() {
        ChatMessage message = ChatMessage.builder()
            .sender("John")
            .content("Hello, World!")
            .build();
            
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(message);

        ChatMessage savedMessage = messageRepository.save(message);

        assertNotNull(savedMessage);
        assertEquals("Hello, World!", savedMessage.getContent());
        verify(messageRepository).save(message);
    }

}
