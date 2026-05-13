package com.example.eventstreamingsystem.exception;

public class TopicNotFoundException extends RuntimeException {
    public TopicNotFoundException(String topic) {
        super("Topic not found: " + topic);
    }
}
