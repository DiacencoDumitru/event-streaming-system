package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.model.StoredEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;

@Service
public class EventStoreService {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<Path, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, AtomicLong> lineCounts = new ConcurrentHashMap<>();

    public EventStoreService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public StoredEvent append(Path partitionFile, String key, String payload) {
        ReentrantLock lock = locks.computeIfAbsent(partitionFile, path -> new ReentrantLock());
        lock.lock();
        try {
            AtomicLong counter = lineCounts.computeIfAbsent(partitionFile, this::createCounterFromFile);
            long nextOffset = counter.get();
            StoredEvent event = new StoredEvent(nextOffset, key, payload, Instant.now().toEpochMilli());
            String serialized = objectMapper.writeValueAsString(event);
            Files.writeString(
                    partitionFile,
                    serialized + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.APPEND
            );
            counter.incrementAndGet();
            return event;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to append event", e);
        } finally {
            lock.unlock();
        }
    }

    public List<StoredEvent> readFrom(Path partitionFile, long offsetInclusive, int maxRecords) {
        ReentrantLock lock = locks.computeIfAbsent(partitionFile, path -> new ReentrantLock());
        lock.lock();
        try {
            List<StoredEvent> result = new ArrayList<>(Math.min(maxRecords, 64));
            try (BufferedReader reader = Files.newBufferedReader(partitionFile, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null && result.size() < maxRecords) {
                    if (line.isBlank()) {
                        continue;
                    }
                    StoredEvent event = objectMapper.readValue(line, new TypeReference<>() {
                    });
                    if (event.offset() >= offsetInclusive) {
                        result.add(event);
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read events", e);
        } finally {
            lock.unlock();
        }
    }

    public long eventCount(Path partitionFile) {
        ReentrantLock lock = locks.computeIfAbsent(partitionFile, path -> new ReentrantLock());
        lock.lock();
        try {
            return lineCounts.computeIfAbsent(partitionFile, this::createCounterFromFile).get();
        } finally {
            lock.unlock();
        }
    }

    private AtomicLong createCounterFromFile(Path partitionFile) {
        return new AtomicLong(countLines(partitionFile));
    }

    private long countLines(Path partitionFile) {
        try {
            if (Files.notExists(partitionFile)) {
                return 0L;
            }
            try (var stream = Files.lines(partitionFile, StandardCharsets.UTF_8)) {
                return stream.filter(line -> !line.isBlank()).count();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to count events", e);
        }
    }
}
