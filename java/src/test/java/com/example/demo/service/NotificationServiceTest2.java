package com.example.demo.service;

import com.example.demo.dto.NotificationRequest;
import com.example.demo.exception.NotificationException;
import com.example.demo.model.Notification;
import com.example.demo.mq.NotificationProducer;
import com.example.demo.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest2 {

    @Mock
    private NotificationRepository repository;

    @Mock
    private NotificationProducer mqProducer; // Using your new interface

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationService notificationService;

    private NotificationRequest createRequest() {
        return new NotificationRequest("email", "test@user.com", "Sub", "Content");
    }

    @Test
    @DisplayName("Test ERR_DB_001: MySQL Save Failure")
    void testProcessNotification_DbFailure() {
        // Arrange: Force repository to throw an exception
        NotificationRequest request = createRequest();
        when(repository.save(any(Notification.class)))
                .thenThrow(new RuntimeException("Database Connection Lost"));

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () -> {
            notificationService.processNotification(request);
        });

        assertEquals(NotificationException.ERR_DB_001, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(NotificationException.MSG_DB_SAVE_FAILED));

        // Verify MQ was never called because DB failed first
        verify(mqProducer, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Test ERR_MQ_003: RocketMQ Send Failure")
    void testProcessNotification_MqFailure() {
        // Arrange: Mock DB success but MQ failure
        NotificationRequest request = createRequest();
        Notification savedNotification = new Notification();
        savedNotification.setId(100L);

        when(repository.save(any(Notification.class))).thenReturn(savedNotification);

        // Force MQ Producer to throw an exception
        doThrow(new RuntimeException("RocketMQ Broker Down"))
                .when(mqProducer).send(anyString(), anyString());

        // Act & Assert
        NotificationException exception = assertThrows(NotificationException.class, () -> {
            notificationService.processNotification(request);
        });

        assertEquals(NotificationException.ERR_MQ_003, exception.getErrorCode());
        assertTrue(exception.getMessage().contains(NotificationException.MSG_MQ_SEND_FAILED));

        // Verify DB save was attempted
        verify(repository, times(1)).save(any(Notification.class));
    }
}