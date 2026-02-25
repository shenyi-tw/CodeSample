package com.example.demo.controller;

import com.example.demo.dto.NotificationRequest;
import com.example.demo.exception.NotificationException;
import com.example.demo.model.Notification;
import com.example.demo.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/recent")
    public ResponseEntity<List<Object>> getRecent() {
        return ResponseEntity.ok(service.getRecentNotifications());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteNotification(id);
        return ResponseEntity.noContent().build(); // Returns 204
    }

    @PutMapping("/{id}")
    public ResponseEntity<Notification> update(@PathVariable Long id, @RequestBody NotificationRequest request) {
        Notification updated = service.updateNotification(id, request);
        return ResponseEntity.ok(updated);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody NotificationRequest request) {
        try {
            Notification notification = service.processNotification(request);
            return ResponseEntity.ok(notification);
        } catch (TransactionSystemException e) {
            // This is where the "Commit Failed" error lands
            log.error("CRITICAL_ERR_001: Database commit failed. RocketMQ message might be orphaned!", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Notification failed at commit level.");
        } catch (NotificationException e) {
            // Handle your custom business exceptions
            log.error("Business Error: {} - {}", e.getErrorCode(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }

    }

    @GetMapping("/{id}")
    public ResponseEntity<Notification> getById(@PathVariable Long id) {
        Notification notification = service.getNotification(id);
        return ResponseEntity.ok(notification);
    }
}
