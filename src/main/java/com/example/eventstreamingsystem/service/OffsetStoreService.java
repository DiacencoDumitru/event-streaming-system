package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.config.StorageProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class OffsetStoreService {

    private final Path offsetsRoot;

    public OffsetStoreService(StorageProperties properties) {
        this.offsetsRoot = Path.of(properties.rootDir()).resolve("offsets");
    }

    public synchronized long readCommittedOffset(String groupId, String topic, int partition) {
        Path file = offsetFile(groupId, topic, partition);
        if (Files.notExists(file)) {
            return -1;
        }
        try {
            String value = Files.readString(file, StandardCharsets.UTF_8).trim();
            if (value.isEmpty()) {
                return -1;
            }
            return Long.parseLong(value);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read committed offset", e);
        }
    }

    public synchronized void commit(String groupId, String topic, int partition, long offset) {
        Path file = offsetFile(groupId, topic, partition);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, Long.toString(offset), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to commit offset", e);
        }
    }

    public synchronized List<String> listGroupIds() {
        if (!Files.isDirectory(offsetsRoot)) {
            return List.of();
        }
        try (var stream = Files.list(offsetsRoot)) {
            List<String> ids = new ArrayList<>();
            stream.filter(Files::isDirectory)
                    .map(p -> p.getFileName().toString())
                    .forEach(ids::add);
            ids.sort(Comparator.naturalOrder());
            return List.copyOf(ids);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list consumer groups", e);
        }
    }

    private Path offsetFile(String groupId, String topic, int partition) {
        return offsetsRoot.resolve(groupId).resolve(topic).resolve("partition-" + partition + ".offset");
    }
}
