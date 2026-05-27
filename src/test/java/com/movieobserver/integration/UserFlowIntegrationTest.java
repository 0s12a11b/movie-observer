package com.movieobserver.integration;

import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.Review;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.ReviewRepository;
import com.movieobserver.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import jakarta.servlet.http.Cookie;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for core user flows.
 *
 * Container lifecycle:
 *   PostgreSQLContainer is started once per JVM (static field in BaseIntegrationTest).
 *   Liquibase migrations (including seed data) run automatically on Spring Boot context start.
 *
 * Test order is intentional: registration creates data that later tests rely on.
 *
 * Seed credentials used:
 *   user@movieobserver.ru    / 123  (ROLE_USER)
 *   critic@movieobserver.ru  / 123  (ROLE_CRITIC)
 *   editor@movieobserver.ru  / 123  (ROLE_EDITOR)
 *
 * Seeded movies:
 *   11111111-1111-1111-1111-111111111111 ("Интерстеллар")
 *   22222222-2222-2222-2222-222222222222 ("Начало")
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("isDockerAvailable")
class UserFlowIntegrationTest extends BaseIntegrationTest {

    /**
     * Checks whether Docker is accessible via TCP on localhost:2375.
     * On Windows + Docker Desktop, enable "Expose daemon on tcp://localhost:2375 without TLS"
     * in Docker Desktop Settings -> General to allow Testcontainers access.
     *
     * @return true if Docker TCP endpoint is reachable
     */
    static boolean isDockerAvailable() {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("localhost", 2375), 500);
            return true;
        } catch (IOException e) {
            System.out.println("[INFO] Docker TCP not available on localhost:2375 — skipping integration tests.");
            System.out.println("[INFO] To enable: Docker Desktop -> Settings -> General -> Expose daemon on tcp://localhost:2375");
            return false;
        }
    }

    // Seed data constants
    private static final String USER_EMAIL   = "user@movieobserver.ru";
    private static final String CRITIC_EMAIL = "critic@movieobserver.ru";
    private static final String EDITOR_EMAIL = "editor@movieobserver.ru";
    private static final String PASSWORD     = "123";

    private static final UUID MOVIE_INTERSTELLAR =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID MOVIE_INCEPTION =
            UUID.fromString("22222222-2222-2222-2222-222222222222");

    // Fresh user registered in test 1
    private static final String NEW_USER_EMAIL    = "newuser_it@movieobserver.ru";
    private static final String NEW_USER_PASSWORD = "test123";

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private UserRepository userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: New user registration
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /register — новый пользователь регистрируется с ролью ROLE_USER")
    void registerNewUser_shouldCreateUserWithRoleUser() throws Exception {
        // Act
        mockMvc.perform(
                post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", NEW_USER_EMAIL)
                        .param("password", NEW_USER_PASSWORD)
                        .param("displayName", "Интеграционный Тестировщик")
        )
        // Assert: successful registration redirects to /login
        .andExpect(status().is3xxRedirection())
        .andExpect(header().string("Location", "/login"));

        // Verify user was persisted with correct role
        var persistedUser = userRepository.findByEmail(NEW_USER_EMAIL);
        assertThat(persistedUser).isPresent();
        assertThat(persistedUser.get().getRoles())
                .extracting(role -> role.getName())
                .containsExactly("ROLE_USER");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Login — seeded user receives JWT cookie
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(2)
    @DisplayName("POST /login — сидированный пользователь получает JWT-cookie")
    void loginSeedUser_shouldReturnJwtCookie() throws Exception {
        Cookie jwtCookie = loginAndGetJwtCookie(USER_EMAIL, PASSWORD);

        assertThat(jwtCookie)
                .as("Login with seeded user should yield a JWT_TOKEN cookie")
                .isNotNull();
        assertThat(jwtCookie.getValue())
                .as("JWT_TOKEN cookie must not be empty")
                .isNotBlank();
        assertThat(jwtCookie.isHttpOnly())
                .as("JWT_TOKEN cookie must be HttpOnly")
                .isTrue();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Editor creates a new movie
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(3)
    @DisplayName("POST /editor/movies/save — редактор создаёт новый фильм")
    void editorCreatesMovie_shouldPersistMovie() throws Exception {
        // Arrange: log in as editor
        Cookie editorCookie = loginAndGetJwtCookie(EDITOR_EMAIL, PASSWORD);
        assertThat(editorCookie).isNotNull();

        long movieCountBefore = movieRepository.count();

        // Act
        mockMvc.perform(
                post("/editor/movies/save")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(editorCookie)
                        .param("title", "Интеграционный Тест: Новый Фильм")
                        .param("description", "Тестовое описание фильма для интеграционного теста")
                        .param("releaseYear", "2024")
                        .param("durationMinutes", "120")
        )
        // Assert: redirect back to dashboard means success
        .andExpect(status().is3xxRedirection());

        // Verify persistence in database
        long movieCountAfter = movieRepository.count();
        assertThat(movieCountAfter)
                .as("One new movie should be created")
                .isEqualTo(movieCountBefore + 1);

        List<Movie> movies = movieRepository.findAll();
        assertThat(movies)
                .extracting(Movie::getTitle)
                .contains("Интеграционный Тест: Новый Фильм");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Regular user submits review — requires moderation (is_approved = false)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(4)
    @DisplayName("POST /movies/{id}/reviews — пользователь отправляет рецензию на модерацию")
    void regularUser_submitReview_shouldNotBeApprovedAutomatically() throws Exception {
        // Arrange: log in as regular user
        Cookie userCookie = loginAndGetJwtCookie(USER_EMAIL, PASSWORD);
        assertThat(userCookie).isNotNull();

        long reviewCountBefore = reviewRepository.count();
        BigDecimal ratingBefore = movieRepository.findById(MOVIE_INTERSTELLAR)
                .map(Movie::getAverageRating)
                .orElse(BigDecimal.ZERO);

        String reviewText = "Это интеграционный тест рецензии пользователя. Текст длиннее 10 символов.";

        // Act
        mockMvc.perform(
                post("/movies/" + MOVIE_INTERSTELLAR + "/reviews")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(userCookie)
                        .param("rating", "8")
                        .param("reviewText", reviewText)
        )
        .andExpect(status().is3xxRedirection());

        // One more review in DB
        long reviewCountAfter = reviewRepository.count();
        assertThat(reviewCountAfter)
                .as("Review count should increase by 1")
                .isEqualTo(reviewCountBefore + 1);

        // Find the new review — it must NOT be approved
        Review savedReview = reviewRepository.findAll().stream()
                .filter(r -> reviewText.equals(r.getReviewText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Review was not persisted"));

        assertThat(savedReview.getIsApproved())
                .as("User review should require moderation (is_approved = false)")
                .isFalse();

        // Movie rating should NOT have changed (review not approved)
        BigDecimal ratingAfter = movieRepository.findById(MOVIE_INTERSTELLAR)
                .map(Movie::getAverageRating)
                .orElse(BigDecimal.ZERO);
        assertThat(ratingAfter)
                .as("Rating must not change when unapproved user review is submitted (was %s)".formatted(ratingBefore))
                .isEqualByComparingTo(ratingBefore);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Critic submits review — auto-approved and rating updates
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("POST /movies/{id}/reviews — критик отправляет рецензию, рейтинг пересчитывается")
    void critic_submitReview_shouldAutoApproveAndUpdateRating() throws Exception {
        // Arrange: log in as critic
        Cookie criticCookie = loginAndGetJwtCookie(CRITIC_EMAIL, PASSWORD);
        assertThat(criticCookie).isNotNull();

        // Use the seeded "Начало" movie to get a predictable starting point
        BigDecimal ratingBefore = movieRepository.findById(MOVIE_INCEPTION)
                .map(Movie::getAverageRating)
                .orElse(BigDecimal.ZERO);

        // Intentionally low rating — will pull the average down from 9.80
        int criticRating = 4;
        String reviewText = "Рецензия кинокритика: тест автоматического одобрения. Очень детальный анализ.";

        // Act
        mockMvc.perform(
                post("/movies/" + MOVIE_INCEPTION + "/reviews")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .cookie(criticCookie)
                        .param("rating", String.valueOf(criticRating))
                        .param("reviewText", reviewText)
        )
        .andExpect(status().is3xxRedirection());

        // Find the saved critic review — it MUST be auto-approved
        Review criticReview = reviewRepository.findAll().stream()
                .filter(r -> reviewText.equals(r.getReviewText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Critic review was not persisted"));

        assertThat(criticReview.getIsApproved())
                .as("Critic review must be auto-approved (is_approved = true)")
                .isTrue();

        // Rating must have been recalculated — new average must differ from old
        BigDecimal ratingAfter = movieRepository.findById(MOVIE_INCEPTION)
                .map(Movie::getAverageRating)
                .orElse(BigDecimal.ZERO);

        assertThat(ratingAfter)
                .as("Movie rating must be recalculated after critic review auto-approval (was %s, critic gave %d)"
                        .formatted(ratingBefore, criticRating))
                .isNotEqualByComparingTo(ratingBefore);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: Actuator Health endpoint
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /actuator/health — проверка работоспособности (Health)")
    void actuatorHealth_shouldReturnUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 7: Actuator Metrics endpoint
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("GET /actuator/metrics — получение списка метрик")
    void actuatorMetrics_shouldReturnMetricsList() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}
