package com.example.demo;

import com.example.demo.dto.NotificationRequest;
import com.example.demo.exception.NotificationException;
import com.example.demo.mq.NotificationProducer;
import com.example.demo.repository.NotificationRepository;
import com.example.demo.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
@SpringBootTest
@ActiveProfiles("test") // This activates application-test.yml
class NotificationIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository repository;

    @MockitoBean
    private NotificationProducer mqProducer;

    @BeforeEach
    void setup() {
        repository.deleteAll();
    }

    @Test
    @DisplayName("Verify MySQL Rollback when RocketMQ Fails")
    void testRollbackOnMqFailure() {
        // Arrange
        NotificationRequest request = new NotificationRequest("email", "test@user.com", "Sub", "Content");

        // Force MQ to fail
        doThrow(new RuntimeException("MQ Down")).when(mqProducer).send(anyString(), anyString());

        // Act
        assertThrows(NotificationException.class, () -> {
            notificationService.processNotification(request);
        });

        // Assert: In H2, this will definitely be 0 if the transaction works
        long count = repository.count();
        assertEquals(0, count, "H2 should be empty due to transaction rollback");
    }
}