package com.example.eventstreamingsystem.controller;

import com.example.eventstreamingsystem.dto.CommitOffsetRequest;
import com.example.eventstreamingsystem.dto.PollResponse;
import com.example.eventstreamingsystem.service.ConsumerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/consumers")
public class ConsumerController {

    private final ConsumerService consumerService;

    public ConsumerController(ConsumerService consumerService) {
        this.consumerService = consumerService;
    }

    @GetMapping("/poll")
    public PollResponse poll(
            @RequestParam @NotBlank String groupId,
            @RequestParam @NotBlank String topic,
            @RequestParam @Min(0) int partition,
            @RequestParam(defaultValue = "100") @Min(1) int maxRecords
    ) {
        return new PollResponse(consumerService.poll(groupId, topic, partition, maxRecords));
    }

    @PostMapping("/commit")
    public void commit(@Valid @RequestBody CommitOffsetRequest request) {
        consumerService.commit(request.groupId(), request.topic(), request.partition(), request.offset());
    }
}
