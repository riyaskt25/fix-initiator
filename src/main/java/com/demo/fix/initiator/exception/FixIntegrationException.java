package com.demo.fix.initiator.exception;

public class FixIntegrationException extends RuntimeException {

    private final String sessionKey;
    private final String messageType;

    public FixIntegrationException(String sessionKey, String messageType, String message) {
        super(message);
        this.sessionKey = sessionKey;
        this.messageType = messageType;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getMessageType() {
        return messageType;
    }

}
