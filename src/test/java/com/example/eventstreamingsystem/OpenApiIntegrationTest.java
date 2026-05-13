package com.example.eventstreamingsystem;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiIntegrationTest {

    private static final Path STORAGE_DIR =
            Path.of(System.getProperty("java.io.tmpdir"), "event-streaming-openapi-it-" + UUID.randomUUID());

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
    void openApiDocumentListsPublicApiPaths() {
        String baseUrl = "http://localhost:" + port;
        String body = restTemplate.getForObject(baseUrl + "/v3/api-docs", String.class);
        assertThat(body).isNotNull();
        assertThat(body).contains("/api/topics");
        assertThat(body).contains("/api/events");
        assertThat(body).contains("/api/consumers");
    }
}
