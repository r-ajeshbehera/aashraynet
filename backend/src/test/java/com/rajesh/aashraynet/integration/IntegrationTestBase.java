package com.rajesh.aashraynet.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Real MySQL 8 in Docker (Testcontainers) + Flyway migrations + demo data. Requires Docker.
 * Test classes share one database, so each test is written to work whatever ran before it.
 * Demo accounts: see db/demo/V2__demo_data.sql (password Demo@1234).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    /**
     * Database for integration tests:
     *  - default (CI): one MySQL 8.4 container for the whole run (Testcontainers, needs Docker)
     *  - TEST_DB_URL / TEST_DB_USER / TEST_DB_PASSWORD set: an existing empty MySQL 8 database instead
     */
    static final String EXTERNAL_URL = System.getenv("TEST_DB_URL");
    static final MySQLContainer<?> MYSQL = EXTERNAL_URL == null ? new MySQLContainer<>("mysql:8.4") : null;

    static {
        if (MYSQL != null) MYSQL.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        if (MYSQL != null) {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("TEST_DB_USER"));
            registry.add("spring.datasource.password", () -> System.getenv("TEST_DB_PASSWORD"));
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;

    protected static final String STATE = "state@aashraynet.demo";
    protected static final String PURI_ADMIN = "puri.admin@aashraynet.demo";
    protected static final String KENDRAPARA_ADMIN = "kendrapara.admin@aashraynet.demo";
    protected static final String SATAPADA_INCHARGE = "satapada.incharge@aashraynet.demo";
    protected static final String PURI_VOLUNTEER = "puri.volunteer@aashraynet.demo";
    protected static final String KENDRAPARA_VOLUNTEER = "kendrapara.volunteer@aashraynet.demo";

    protected String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Demo@1234\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("accessToken").asText();
    }

    protected JsonNode read(String body) throws Exception {
        return json.readTree(body);
    }
}
