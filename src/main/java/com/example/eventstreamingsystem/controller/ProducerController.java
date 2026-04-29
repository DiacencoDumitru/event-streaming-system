package com.example.eventstreamingsystem.controller;

import com.example.eventstreamingsystem.dto.PublishEventRequest;
import com.example.eventstreamingsystem.model.StoredEvent;
import com.example.eventstreamingsystem.service.ProducerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class ProducerController {

    private final ProducerService producerService;

    public ProducerController(ProducerService producerService) {
        this.producerService = producerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StoredEvent publish(@Valid @RequestBody PublishEventRequest request) {
        return producerService.publish(request.topic(), request.key(), request.payload());
    }
}
