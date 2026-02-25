package com.example.demo.dto;

public record NotificationRequest(
        String type,
        String recipient,
        String subject,
        String content
) {}