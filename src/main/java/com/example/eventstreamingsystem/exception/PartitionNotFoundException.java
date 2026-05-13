package com.example.eventstreamingsystem.exception;

public class PartitionNotFoundException extends RuntimeException {
    public PartitionNotFoundException(String topic, int partition) {
        super("Partition not found: " + topic + "/" + partition);
    }
}
