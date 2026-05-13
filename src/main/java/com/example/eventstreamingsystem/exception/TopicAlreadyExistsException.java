package com.example.eventstreamingsystem.exception;

public class TopicAlreadyExistsException extends RuntimeException {
    public TopicAlreadyExistsException(String topic) {
        super("Topic already exists: " + topic);
    }
}
