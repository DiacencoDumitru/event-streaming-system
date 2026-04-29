package com.example.eventstreamingsystem.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateTopicRequest(@NotBlank String name, @Min(1) int partitions) {
}
