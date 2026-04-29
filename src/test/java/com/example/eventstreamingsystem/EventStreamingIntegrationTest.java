package com.example.eventstreamingsystem;

import com.example.eventstreamingsystem.dto.CommitOffsetRequest;
import com.example.eventstreamingsystem.dto.CreateTopicRequest;
import com.example.eventstreamingsystem.dto.PollResponse;
import com.example.eventstreamingsystem.dto.PublishEventRequest;
import com.example.eventstreamingsystem.model.StoredEvent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
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
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

        ResponseEntity<StoredEvent> publishResponse = restTemplate.postForEntity(
                baseUrl + "/api/events",
                new PublishEventRequest("orders", null, "order-created"),
                StoredEvent.class
        );
        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(Objects.requireNonNull(publishResponse.getBody()).offset()).isZero();

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
