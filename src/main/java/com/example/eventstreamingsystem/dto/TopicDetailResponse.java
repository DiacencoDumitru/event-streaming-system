package com.example.eventstreamingsystem.dto;

import java.util.List;

public record TopicDetailResponse(String name, int partitions, List<PartitionStatsResponse> partitionStats) {
}
