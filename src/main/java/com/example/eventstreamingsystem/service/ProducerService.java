package com.example.eventstreamingsystem.service;

import com.example.eventstreamingsystem.dto.PublishBatchEventItem;
import com.example.eventstreamingsystem.model.StoredEvent;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProducerService {

    private final TopicService topicService;
    private final EventStoreService eventStoreService;

    public ProducerService(TopicService topicService, EventStoreService eventStoreService) {
        this.topicService = topicService;
        this.eventStoreService = eventStoreService;
    }

    public StoredEvent publish(String topic, String key, String payload) {
        int partition = topicService.selectPartition(topic, key);
        Path partitionFile = topicService.partitionFile(topic, partition);
        return eventStoreService.append(partitionFile, key, payload);
    }

    public List<StoredEvent> publishBatch(String topic, List<PublishBatchEventItem> events) {
        List<StoredEvent> results = new ArrayList<>(events.size());
        for (PublishBatchEventItem item : events) {
            results.add(publish(topic, item.key(), item.payload()));
        }
        return results;
    }
}
