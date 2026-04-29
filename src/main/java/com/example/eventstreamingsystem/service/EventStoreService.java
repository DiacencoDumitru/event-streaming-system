package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.model.StoredEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class EventStoreService {

    private final ObjectMapper objectMapper;

    public EventStoreService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public synchronized StoredEvent append(Path partitionFile, String key, String payload) {
        try {
            List<String> lines = Files.readAllLines(partitionFile, StandardCharsets.UTF_8);
            long nextOffset = lines.size();
            StoredEvent event = new StoredEvent(nextOffset, key, payload, Instant.now().toEpochMilli());
            String serialized = objectMapper.writeValueAsString(event);
            Files.writeString(
                    partitionFile,
                    serialized + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
            return event;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append event", e);
        }
    }

    public synchronized List<StoredEvent> readFrom(Path partitionFile, long offsetInclusive, int maxRecords) {
        try {
            List<String> lines = Files.readAllLines(partitionFile, StandardCharsets.UTF_8);
            List<StoredEvent> allEvents = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (!line.isBlank()) {
                    allEvents.add(objectMapper.readValue(line, new TypeReference<>() {
                    }));
                }
            }
            List<StoredEvent> result = new ArrayList<>();
            for (StoredEvent event : allEvents) {
                if (event.offset() >= offsetInclusive) {
                    result.add(event);
                }
                if (result.size() == maxRecords) {
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read events", e);
        }
    }
}
