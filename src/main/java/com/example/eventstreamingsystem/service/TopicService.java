package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.config.StorageProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class TopicService {

    private final Path topicsRoot;

    public TopicService(StorageProperties properties) {
        this.topicsRoot = Path.of(properties.rootDir()).resolve("topics");
    }

    public void createTopic(String topic, int partitions) {
        try {
            Path topicDir = topicsRoot.resolve(topic);
            Files.createDirectories(topicDir);
            for (int i = 0; i < partitions; i++) {
                Path partitionFile = topicDir.resolve(partitionFileName(i));
                if (Files.notExists(partitionFile)) {
                    Files.createFile(partitionFile);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create topic " + topic, e);
        }
    }

    public Path partitionFile(String topic, int partition) {
        Path partitionPath = topicsRoot.resolve(topic).resolve(partitionFileName(partition));
        if (Files.notExists(partitionPath)) {
            throw new IllegalArgumentException("Topic or partition does not exist");
        }
        return partitionPath;
    }

    public int selectPartition(String topic, String key) {
        Path topicDir = topicsRoot.resolve(topic);
        try (var stream = Files.list(topicDir)) {
            int partitions = (int) stream.filter(path -> path.getFileName().toString().startsWith("partition-")).count();
            if (partitions == 0) {
                throw new IllegalArgumentException("Topic has no partitions");
            }
            if (key == null || key.isBlank()) {
                return 0;
            }
            return Math.floorMod(key.hashCode(), partitions);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve topic partitions", e);
        }
    }

    private String partitionFileName(int partition) {
        return "partition-" + partition + ".log";
    }
}
