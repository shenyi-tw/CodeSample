package com.example.demo.service;

import com.example.demo.cache.NotificationCache;
import com.example.demo.dto.NotificationRequest;
import com.example.demo.exception.NotificationException;
import com.example.demo.model.Notification;
import com.example.demo.mq.NotificationProducer;
import com.example.demo.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationProducer mqProducer;
    private final NotificationCache notificationCache;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "notification-topic";

    public List<Object> getRecentNotifications() {
        try {
            return notificationCache.getRecent(10);
        } catch (Exception e) {
            log.error("ERR_REDIS_009: Failed to fetch recent list", e);
            return Collections.emptyList();
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long id) {
        if (!repository.existsById(id)) {
            throw new NotificationException(NotificationException.ERR_NOT_FOUND, NotificationException.MSG_NOT_FOUND);
        }

        repository.deleteById(id);

        try {
            notificationCache.delete("notification:" + id);
            notificationCache.clearRecent();

            log.debug("Deleted notification and cleared cache for ID: {}", id);
        } catch (Exception e) {
            log.error("ERR_REDIS_008: Failed to clear cache during deletion", e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Notification updateNotification(Long id, NotificationRequest request) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationException(NotificationException.ERR_NOT_FOUND, NotificationException.MSG_NOT_FOUND));

        notification.setSubject(request.subject());
        notification.setContent(request.content());
        notification = repository.save(notification);

        try {
            notificationCache.clearRecent();
            log.debug("Cache invalidated for notification: {}", id);
        } catch (Exception e) {
            log.error("ERR_REDIS_007: Failed to invalidate cache", e);
        }

        return notification;
    }

    public Notification getNotification(Long id) {
        String redisKey = "notification:" + id;
        log.trace("getNotification {}", redisKey);

        try {
            Object cached = notificationCache.get(redisKey);
            if (cached != null) {
                log.trace("cached != null {}", redisKey);
                return (Notification) cached;
            }
        } catch (Exception e) {
            log.error("ERR_REDIS_005: Cache read failed", e);
        }

        return repository.findById(id)
                .orElseThrow(() -> new NotificationException(NotificationException.ERR_NOT_FOUND, NotificationException.MSG_NOT_FOUND));
    }

    @Transactional(rollbackFor = Exception.class)
    public Notification processNotification(NotificationRequest request) {
        Notification notification;
        try {
            notification = new Notification();
            notification.setType(request.type());
            notification.setRecipient(request.recipient());
            notification.setSubject(request.subject());
            notification.setContent(request.content());
            notification = repository.save(notification);
            log.debug("Step 1: MySQL save success for ID: {}", notification.getId());
        } catch (Exception e) {
            throw new NotificationException(NotificationException.ERR_DB_001, NotificationException.MSG_DB_SAVE_FAILED, e);
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new NotificationException(NotificationException.ERR_JSON_002, NotificationException.MSG_JSON_SERIALIZE_FAILED, e);
        }

        try {
            mqProducer.send(TOPIC, jsonPayload);
            log.debug("Step 2: RocketMQ send success");
        } catch (Exception e) {
            throw new NotificationException(NotificationException.ERR_MQ_003, NotificationException.MSG_MQ_SEND_FAILED, e);
        }

        try {
            notificationCache.pushToRecentAtomic(notification);
            log.debug("Step 3: Redis cache success");
        } catch (Exception e) {
            log.error("ERR_REDIS_004: Redis async cache failed", e);
        }

        return notification;
    }
}
