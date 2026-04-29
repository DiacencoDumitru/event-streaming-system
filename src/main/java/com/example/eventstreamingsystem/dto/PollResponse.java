package com.example.eventstreamingsystem.dto;

import com.example.eventstreamingsystem.model.StoredEvent;

import java.util.List;

public record PollResponse(List<StoredEvent> records) {
}
