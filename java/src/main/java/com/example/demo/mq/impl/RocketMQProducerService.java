package com.example.demo.mq.impl;

import com.example.demo.exception.NotificationException;
import com.example.demo.mq.NotificationProducer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;

@Service
public class RocketMQProducerService implements NotificationProducer {

    private DefaultMQProducer producer;
    @Value("${ROCKETMQ_NAMESRV:rocketmq-namesrv:9876}")
    private String namesrvAddr;

    @PostConstruct
    public void init() throws Exception {
        producer = new DefaultMQProducer("notification_group");
        producer.setNamesrvAddr(namesrvAddr);
        producer.start();
    }

    public void send(String topic, String jsonBody) {
        try {
            // Create the message with the specified topic and a default tag
            Message msg = new Message(topic, "TagA", jsonBody.getBytes(StandardCharsets.UTF_8));

            // Attempt to send the message
            producer.send(msg);

        } catch (org.apache.rocketmq.client.exception.MQClientException e) {
            // Specific to client-side issues (e.g., producer not started)
            throw new NotificationException("ERR_MQ_CLIENT", "RocketMQ client is not initialized or configured incorrectly", e);

        } catch (org.apache.rocketmq.remoting.exception.RemotingException e) {
            // Specific to network/connection issues with the Broker
            throw new NotificationException("ERR_MQ_NETWORK", "Network error occurred while connecting to RocketMQ Broker", e);

        } catch (org.apache.rocketmq.client.exception.MQBrokerException e) {
            // Specific to broker-side issues (e.g., topic doesn't exist, permission denied)
            throw new NotificationException("ERR_MQ_BROKER", "RocketMQ Broker rejected the message. Check if topic exists.", e);

        } catch (InterruptedException e) {
            // Thread was interrupted during the send operation
            Thread.currentThread().interrupt();
            throw new NotificationException("ERR_MQ_INTERRUPT", "The message sending thread was interrupted", e);

        } catch (Exception e) {
            // Catch-all for any other unexpected errors
            throw new NotificationException("ERR_MQ_UNKNOWN", "An unexpected error occurred in RocketMQ Producer", e);
        }
    }

    @PreDestroy
    public void stop() {
        if (producer != null) producer.shutdown();
    }
}