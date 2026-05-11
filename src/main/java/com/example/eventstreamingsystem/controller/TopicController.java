package com.example.eventstreamingsystem.controller;

import com.example.eventstreamingsystem.dto.CreateTopicRequest;
import com.example.eventstreamingsystem.dto.TopicSummaryResponse;
import com.example.eventstreamingsystem.service.TopicService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    public List<TopicSummaryResponse> list() {
        return topicService.listTopics();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateTopicRequest request) {
        topicService.createTopic(request.name(), request.partitions());
    }
}
