package com.example.eventstreamingsystem.dto;

import java.util.List;

public record ConsumerLagResponse(String groupId, String topic, List<PartitionLagResponse> partitions) {
}
