package com.example.demo.exception;

import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException {
    private final String errorCode;

    public static final String ERR_NOT_FOUND = "ERR_DB_002";
    public static final String MSG_NOT_FOUND = "Notification not found";

    public static final String ERR_DB_001 = "ERR_DB_001";
    public static final String ERR_JSON_002 = "ERR_JSON_002";
    public static final String ERR_MQ_003 = "ERR_MQ_003";

    public static final String MSG_DB_SAVE_FAILED = "Failed to save to MySQL";
    public static final String MSG_JSON_SERIALIZE_FAILED = "serialization failed";
    public static final String MSG_MQ_SEND_FAILED = "RocketMQ push failed";

    public NotificationException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public NotificationException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
