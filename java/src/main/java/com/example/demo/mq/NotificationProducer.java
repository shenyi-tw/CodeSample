package com.example.demo.mq;

public interface NotificationProducer {
    /**
     * Sends a message to a specific topic.
     * @param topic Target topic name
     * @param payload JSON string or message body
     */
    void send(String topic, String payload);
}
