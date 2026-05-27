package com.movieobserver.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Abstract base class for integration tests.
 *
 * Uses Testcontainers to spin up a real PostgreSQL 16 instance, and @ServiceConnection
 * to automatically bind it as the application datasource. Liquibase migrations run
 * on startup, so all tables and seed data are present for every test.
 *
 * Uses MockMvc (via @AutoConfigureMockMvc) for HTTP call simulation —
 * the recommended approach in Spring Boot 4.x where TestRestTemplate was removed.
 *
 * Provides helper methods:
 *  - loginAndGetJwtCookie(email, password) – logs in and returns the JWT_TOKEN Cookie object
 *
 * NOTE: Tests are enabled only when Docker is reachable (checked via isDockerAvailable()).
 * To run these tests on Windows + Docker Desktop, you must enable TCP socket:
 * Docker Desktop -> Settings -> General -> "Expose daemon on tcp://localhost:2375 without TLS"
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("movie_observer_test")
                    .withUsername("postgres")
                    .withPassword("123");

    @Autowired
    protected MockMvc mockMvc;

    /**
     * Performs a form-based POST /login, then extracts the JWT_TOKEN Cookie
     * from the Set-Cookie response header.
     *
     * @return Cookie object with name=JWT_TOKEN, or null if login failed / cookie not found
     */
    protected Cookie loginAndGetJwtCookie(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", email)
                        .param("password", password)
        ).andExpect(status().is3xxRedirection()).andReturn();

        MockHttpServletResponse response = result.getResponse();
        Cookie[] cookies = response.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("JWT_TOKEN".equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
}
