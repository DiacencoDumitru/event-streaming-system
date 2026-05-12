package com.example.eventstreamingsystem;

import com.example.eventstreamingsystem.dto.CommitOffsetRequest;
import com.example.eventstreamingsystem.dto.ConsumerGroupsResponse;
import com.example.eventstreamingsystem.dto.ConsumerLagResponse;
import com.example.eventstreamingsystem.dto.CreateTopicRequest;
import com.example.eventstreamingsystem.dto.PartitionLagResponse;
import com.example.eventstreamingsystem.dto.PartitionStatsResponse;
import com.example.eventstreamingsystem.dto.PollResponse;
import com.example.eventstreamingsystem.dto.PublishEventRequest;
import com.example.eventstreamingsystem.dto.TopicDetailResponse;
import com.example.eventstreamingsystem.dto.TopicSummaryResponse;
import com.example.eventstreamingsystem.model.StoredEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EventStreamingIntegrationTest {

    private static final Path STORAGE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "event-streaming-it-" + UUID.randomUUID());

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("app.storage.root-dir", () -> STORAGE_DIR.toString());
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (Files.notExists(STORAGE_DIR)) {
            return;
        }
        try (var stream = Files.walk(STORAGE_DIR)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    @Test
    void shouldCreateTopicPublishPollCommitAndPollAgain() {
        String baseUrl = "http://localhost:" + port;

        ResponseEntity<Void> createTopicResponse = restTemplate.postForEntity(
                baseUrl + "/api/topics",
                new CreateTopicRequest("orders", 2),
                Void.class
        );
        assertThat(createTopicResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Void> createSecondTopicResponse = restTemplate.postForEntity(
                baseUrl + "/api/topics",
                new CreateTopicRequest("events", 1),
                Void.class
        );
        assertThat(createSecondTopicResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<List<TopicSummaryResponse>> listTopicsResponse = restTemplate.exchange(
                baseUrl + "/api/topics",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {
                }
        );
        assertThat(listTopicsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(listTopicsResponse.getBody())).hasSize(2);
        assertThat(listTopicsResponse.getBody())
                .extracting(TopicSummaryResponse::name, TopicSummaryResponse::partitions)
                .containsExactly(tuple("events", 1), tuple("orders", 2));

        ResponseEntity<StoredEvent> publishResponse = restTemplate.postForEntity(
                baseUrl + "/api/events",
                new PublishEventRequest("orders", null, "order-created"),
                StoredEvent.class
        );
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(publishResponse.getBody()).offset()).isZero();

        ResponseEntity<TopicDetailResponse> topicDetailResponse = restTemplate.getForEntity(
                baseUrl + "/api/topics/orders",
                TopicDetailResponse.class
        );
        assertThat(topicDetailResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        TopicDetailResponse detail = Objects.requireNonNull(topicDetailResponse.getBody());
        assertThat(detail.name()).isEqualTo("orders");
        assertThat(detail.partitions()).isEqualTo(2);
        assertThat(detail.partitionStats())
                .extracting(PartitionStatsResponse::partition, PartitionStatsResponse::eventCount)
                .containsExactly(tuple(0, 1L), tuple(1, 0L));

        ResponseEntity<Void> missingTopicResponse = restTemplate.getForEntity(
                baseUrl + "/api/topics/unknown-topic",
                Void.class
        );
        assertThat(missingTopicResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<Void> missingLagResponse = restTemplate.getForEntity(
                baseUrl + "/api/consumers/lag?groupId=group-a&topic=unknown-topic",
                Void.class
        );
        assertThat(missingLagResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        ResponseEntity<ConsumerGroupsResponse> groupsBeforeCommit = restTemplate.getForEntity(
                baseUrl + "/api/consumers/groups",
                ConsumerGroupsResponse.class
        );
        assertThat(groupsBeforeCommit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(groupsBeforeCommit.getBody()).groupIds()).isEmpty();

        ResponseEntity<ConsumerLagResponse> lagBeforeConsume = restTemplate.getForEntity(
                baseUrl + "/api/consumers/lag?groupId=group-a&topic=orders",
                ConsumerLagResponse.class
        );
        assertThat(lagBeforeConsume.getStatusCode()).isEqualTo(HttpStatus.OK);
        ConsumerLagResponse lagBefore = Objects.requireNonNull(lagBeforeConsume.getBody());
        assertThat(lagBefore.groupId()).isEqualTo("group-a");
        assertThat(lagBefore.topic()).isEqualTo("orders");
        assertThat(lagBefore.partitions())
                .extracting(
                        PartitionLagResponse::partition,
                        PartitionLagResponse::committedOffset,
                        PartitionLagResponse::endOffsetInclusive,
                        PartitionLagResponse::lag
                )
                .containsExactly(
                        tuple(0, -1L, 0L, 1L),
                        tuple(1, -1L, -1L, 0L)
                );

        ResponseEntity<PollResponse> firstPollResponse = restTemplate.getForEntity(
                baseUrl + "/api/consumers/poll?groupId=group-a&topic=orders&partition=0&maxRecords=10",
                PollResponse.class
        );
        assertThat(firstPollResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(firstPollResponse.getBody()).records()).hasSize(1);
        assertThat(firstPollResponse.getBody().records().getFirst().payload()).isEqualTo("order-created");

        ResponseEntity<Void> commitResponse = restTemplate.postForEntity(
                baseUrl + "/api/consumers/commit",
                new CommitOffsetRequest("group-a", "orders", 0, 0),
                Void.class
        );
        assertThat(commitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<ConsumerLagResponse> lagAfterCommit = restTemplate.getForEntity(
                baseUrl + "/api/consumers/lag?groupId=group-a&topic=orders",
                ConsumerLagResponse.class
        );
        assertThat(lagAfterCommit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(lagAfterCommit.getBody()).partitions())
                .extracting(
                        PartitionLagResponse::partition,
                        PartitionLagResponse::committedOffset,
                        PartitionLagResponse::endOffsetInclusive,
                        PartitionLagResponse::lag
                )
                .containsExactly(
                        tuple(0, 0L, 0L, 0L),
                        tuple(1, -1L, -1L, 0L)
                );

        ResponseEntity<ConsumerGroupsResponse> groupsAfterCommit = restTemplate.getForEntity(
                baseUrl + "/api/consumers/groups",
                ConsumerGroupsResponse.class
        );
        assertThat(groupsAfterCommit.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(groupsAfterCommit.getBody()).groupIds()).containsExactly("group-a");

        ResponseEntity<PollResponse> secondPollResponse = restTemplate.exchange(
                baseUrl + "/api/consumers/poll?groupId=group-a&topic=orders&partition=0&maxRecords=10",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                PollResponse.class
        );
        assertThat(secondPollResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(Objects.requireNonNull(secondPollResponse.getBody()).records()).isEmpty();
    }
}
