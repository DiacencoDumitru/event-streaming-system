package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.dto.ConsumerLagResponse;
import com.example.eventstreamingsystem.dto.PartitionLagResponse;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConsumerObservabilityService {

    private final TopicService topicService;
    private final EventStoreService eventStoreService;
    private final OffsetStoreService offsetStoreService;

    public ConsumerObservabilityService(
            TopicService topicService,
            EventStoreService eventStoreService,
            OffsetStoreService offsetStoreService
    ) {
        this.topicService = topicService;
        this.eventStoreService = eventStoreService;
        this.offsetStoreService = offsetStoreService;
    }

    public Optional<ConsumerLagResponse> lag(String groupId, String topic) {
        Optional<Integer> countOpt = topicService.topicPartitionCount(topic);
        if (countOpt.isEmpty()) {
            return Optional.empty();
        }
        int partitionCount = countOpt.get();
        List<PartitionLagResponse> rows = new ArrayList<>(partitionCount);
        for (int i = 0; i < partitionCount; i++) {
            Path partitionFile = topicService.partitionFile(topic, i);
            long eventCount = eventStoreService.eventCount(partitionFile);
            long committed = offsetStoreService.readCommittedOffset(groupId, topic, i);
            long endInclusive = eventCount > 0 ? eventCount - 1 : -1;
            long lag = Math.max(0, eventCount - (committed + 1));
            rows.add(new PartitionLagResponse(i, committed, endInclusive, lag));
        }
        return Optional.of(new ConsumerLagResponse(groupId, topic, List.copyOf(rows)));
    }

    public List<String> listGroupIds() {
        return offsetStoreService.listGroupIds();
    }
}
