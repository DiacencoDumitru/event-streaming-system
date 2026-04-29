package com.example.eventstreamingsystem.model;

public record StoredEvent(long offset, String key, String payload, long createdAtEpochMs) {
}
