package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.model.StoredEvent;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

@Service
public class ConsumerService {

    private final TopicService topicService;
    private final EventStoreService eventStoreService;
    private final OffsetStoreService offsetStoreService;

    public ConsumerService(TopicService topicService, EventStoreService eventStoreService, OffsetStoreService offsetStoreService) {
        this.topicService = topicService;
        this.eventStoreService = eventStoreService;
        this.offsetStoreService = offsetStoreService;
    }

    public List<StoredEvent> poll(String groupId, String topic, int partition, int maxRecords) {
        long committedOffset = offsetStoreService.readCommittedOffset(groupId, topic, partition);
        long startOffset = committedOffset + 1;
        Path partitionFile = topicService.partitionFile(topic, partition);
        return eventStoreService.readFrom(partitionFile, startOffset, maxRecords);
    }

    public void commit(String groupId, String topic, int partition, long offset) {
        topicService.partitionFile(topic, partition);
        offsetStoreService.commit(groupId, topic, partition, offset);
    }

    public void seek(String groupId, String topic, int partition, long nextOffsetInclusive) {
        topicService.partitionFile(topic, partition);
        offsetStoreService.commit(groupId, topic, partition, nextOffsetInclusive - 1);
    }
}
