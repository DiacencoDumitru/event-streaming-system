package com.example.eventstreamingsystem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PublishBatchRequest(
        @NotBlank
        @Pattern(regexp = "^[a-zA-Z0-9._-]{1,128}$")
        String topic,
        @NotEmpty
        @Size(max = 256)
        @Valid
        List<PublishBatchEventItem> events
) {
}
