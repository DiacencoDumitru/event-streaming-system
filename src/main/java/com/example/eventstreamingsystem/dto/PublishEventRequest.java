package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.NotBlank;

public record PublishEventRequest(@NotBlank String topic, String key, @NotBlank String payload) {
}
