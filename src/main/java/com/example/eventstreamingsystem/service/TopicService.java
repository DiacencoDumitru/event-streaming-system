package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.config.StorageProperties;
import com.example.eventstreamingsystem.dto.PartitionStatsResponse;
import com.example.eventstreamingsystem.dto.TopicDetailResponse;
import com.example.eventstreamingsystem.dto.TopicSummaryResponse;
import com.example.eventstreamingsystem.exception.PartitionNotFoundException;
import com.example.eventstreamingsystem.exception.TopicAlreadyExistsException;
import com.example.eventstreamingsystem.exception.TopicNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class TopicService {

    private final Path topicsRoot;
    private final EventStoreService eventStoreService;

    public TopicService(StorageProperties properties, EventStoreService eventStoreService) {
        this.topicsRoot = Path.of(properties.rootDir()).resolve("topics");
        this.eventStoreService = eventStoreService;
    }

    public void createTopic(String topic, int partitions) {
        try {
            Path topicDir = topicsRoot.resolve(topic);
            if (Files.isDirectory(topicDir)) {
                throw new TopicAlreadyExistsException(topic);
            }
            Files.createDirectories(topicDir);
            for (int i = 0; i < partitions; i++) {
                Path partitionFile = topicDir.resolve(partitionFileName(i));
                Files.createFile(partitionFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create topic " + topic, e);
        }
    }

    public List<TopicSummaryResponse> listTopics() {
        if (!Files.isDirectory(topicsRoot)) {
            return List.of();
        }
        try (var stream = Files.list(topicsRoot)) {
            return stream
                    .filter(Files::isDirectory)
                    .map(dir -> new TopicSummaryResponse(dir.getFileName().toString(), countPartitionLogs(dir)))
                    .sorted(Comparator.comparing(TopicSummaryResponse::name))
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list topics", e);
        }
    }

    public Optional<TopicDetailResponse> findTopicDetail(String name) {
        Path topicDir = topicsRoot.resolve(name);
        if (!Files.isDirectory(topicDir)) {
            return Optional.empty();
        }
        int partitionCount = countPartitionLogs(topicDir);
        List<PartitionStatsResponse> stats = new ArrayList<>(partitionCount);
        for (int i = 0; i < partitionCount; i++) {
            Path file = topicDir.resolve(partitionFileName(i));
            long count = Files.isRegularFile(file) ? eventStoreService.eventCount(file) : 0L;
            stats.add(new PartitionStatsResponse(i, count));
        }
        return Optional.of(new TopicDetailResponse(name, partitionCount, List.copyOf(stats)));
    }

    public Optional<Integer> topicPartitionCount(String topic) {
        Path topicDir = topicsRoot.resolve(topic);
        if (!Files.isDirectory(topicDir)) {
            return Optional.empty();
        }
        int n = countPartitionLogs(topicDir);
        if (n == 0) {
            return Optional.empty();
        }
        return Optional.of(n);
    }

    public Path partitionFile(String topic, int partition) {
        Path topicDir = topicsRoot.resolve(topic);
        if (Files.notExists(topicDir)) {
            throw new TopicNotFoundException(topic);
        }
        Path partitionPath = topicDir.resolve(partitionFileName(partition));
        if (Files.notExists(partitionPath)) {
            throw new PartitionNotFoundException(topic, partition);
        }
        return partitionPath;
    }

    public int selectPartition(String topic, String key) {
        Path topicDir = topicsRoot.resolve(topic);
        if (Files.notExists(topicDir)) {
            throw new TopicNotFoundException(topic);
        }
        try (var stream = Files.list(topicDir)) {
            int partitions = (int) stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(this::isPartitionLogFile)
                    .count();
            if (partitions == 0) {
                throw new TopicNotFoundException(topic);
            }
            if (key == null || key.isBlank()) {
                return 0;
            }
            return Math.floorMod(key.hashCode(), partitions);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to resolve topic partitions", e);
        }
    }

    private int countPartitionLogs(Path topicDir) {
        try (var stream = Files.list(topicDir)) {
            return (int) stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(this::isPartitionLogFile)
                    .count();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to count partitions", e);
        }
    }

    private boolean isPartitionLogFile(String fileName) {
        return fileName.startsWith("partition-") && fileName.endsWith(".log");
    }

    private String partitionFileName(int partition) {
        return "partition-" + partition + ".log";
    }
}
