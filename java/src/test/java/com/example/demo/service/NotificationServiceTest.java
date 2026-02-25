package com.example.demo.service;

import com.example.demo.exception.NotificationException;
import com.example.demo.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository; // Mocking the interface

    @InjectMocks
    private NotificationService service;

    @Test
    void testDelete_NotFound_ThrowsException() {
        when(repository.existsById(999L)).thenReturn(false);

        assertThrows(NotificationException.class, () -> {
            service.deleteNotification(999L);
        });
    }
}
