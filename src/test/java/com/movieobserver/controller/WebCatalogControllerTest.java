package com.movieobserver.controller;

import com.movieobserver.domain.entity.*;
import com.movieobserver.repository.*;
import com.movieobserver.security.JwtProvider;
import com.movieobserver.service.CustomUserDetailsService;
import com.movieobserver.service.MovieService;
import com.movieobserver.service.ReviewService;
import com.movieobserver.config.SecurityConfig;
import com.movieobserver.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebCatalogController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class WebCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreRepository genreRepository;

    @MockitoBean
    private ArticleRepository articleRepository;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private MovieCastRepository movieCastRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void testHomepage() throws Exception {
        Movie movie1 = Movie.builder().id(UUID.randomUUID()).title("Фильм 1").averageRating(BigDecimal.valueOf(8.5)).build();
        Movie movie2 = Movie.builder().id(UUID.randomUUID()).title("Фильм 2").averageRating(BigDecimal.valueOf(9.0)).build();

        User author = User.builder().displayName("Автор").build();
        Article article = Article.builder()
                .id(UUID.randomUUID())
                .title("Статья")
                .status(Article.ArticleStatus.PUBLISHED)
                .author(author)
                .build();

        when(movieService.findAll()).thenReturn(List.of(movie1, movie2));
        when(articleRepository.findByStatusOrderByCreatedAtDesc(Article.ArticleStatus.PUBLISHED)).thenReturn(List.of(article));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("articles"))
                .andExpect(model().attributeExists("trendingMovies"));
    }

    @Test
    public void testMoviesList() throws Exception {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Начало").build();
        Genre genre = Genre.builder().id(1L).name("Фантастика").build();

        when(movieService.searchMovies("Начало", 1L)).thenReturn(List.of(movie));
        when(genreRepository.findAll()).thenReturn(List.of(genre));

        mockMvc.perform(get("/movies")
                .param("title", "Начало")
                .param("genreId", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/list"))
                .andExpect(model().attribute("searchTitle", "Начало"))
                .andExpect(model().attribute("selectedGenreId", 1L))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("genres"));
    }

    @Test
    public void testMovieDetail_Found() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Начало").build();

        when(movieService.findById(movieId)).thenReturn(Optional.of(movie));
        when(movieCastRepository.findByMovieId(movieId)).thenReturn(List.of());
        when(reviewService.findApprovedByMovieId(movieId)).thenReturn(List.of());

        mockMvc.perform(get("/movies/" + movieId))
                .andExpect(status().isOk())
                .andExpect(view().name("movies/detail"))
                .andExpect(model().attribute("movie", movie));
    }

    @Test
    public void testMovieDetail_NotFound() throws Exception {
        UUID movieId = UUID.randomUUID();
        when(movieService.findById(movieId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/movies/" + movieId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testArticleDetail_Published() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().displayName("Автор").build();
        Article article = Article.builder()
                .id(articleId)
                .title("Статья")
                .status(Article.ArticleStatus.PUBLISHED)
                .author(author)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(get("/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(view().name("articles/detail"))
                .andExpect(model().attribute("article", article));
    }

    @Test
    public void testArticleDetail_DraftNotFound() throws Exception {
        UUID articleId = UUID.randomUUID();
        Article article = Article.builder().id(articleId).title("Статья").status(Article.ArticleStatus.DRAFT).build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(get("/articles/" + articleId))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testAddReview_Unauthorized() throws Exception {
        UUID movieId = UUID.randomUUID();

        mockMvc.perform(post("/movies/" + movieId + "/reviews")
                .with(csrf())
                .param("rating", "8")
                .param("reviewText", "Хороший фильм для просмотра."))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.ru")
    public void testAddReview_Authorized_Success() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Начало").build();
        User user = User.builder().email("user@test.ru").build();
        Review review = Review.builder().isApproved(true).build();

        when(movieService.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findByEmail("user@test.ru")).thenReturn(Optional.of(user));
        when(reviewService.submitReview(eq(movieId), eq(user), eq(8), eq("Хороший фильм для просмотра."))).thenReturn(review);

        mockMvc.perform(post("/movies/" + movieId + "/reviews")
                .with(csrf())
                .param("rating", "8")
                .param("reviewText", "Хороший фильм для просмотра."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movieId))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(username = "user@test.ru")
    public void testAddReview_ValidationError_Rating() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Начало").build();
        User user = User.builder().email("user@test.ru").build();

        when(movieService.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findByEmail("user@test.ru")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/movies/" + movieId + "/reviews")
                .with(csrf())
                .param("rating", "12")
                .param("reviewText", "Хороший фильм для просмотра."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movieId))
                .andExpect(flash().attribute("errorMessage", "Оценка должна быть от 1 до 10"));
    }

    @Test
    @WithMockUser(username = "user@test.ru")
    public void testAddReview_ValidationError_Text() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Начало").build();
        User user = User.builder().email("user@test.ru").build();

        when(movieService.findById(movieId)).thenReturn(Optional.of(movie));
        when(userRepository.findByEmail("user@test.ru")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/movies/" + movieId + "/reviews")
                .with(csrf())
                .param("rating", "8")
                .param("reviewText", "Коротко"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/movies/" + movieId))
                .andExpect(flash().attribute("errorMessage", "Текст рецензии должен содержать не менее 10 символов"));
    }
}
