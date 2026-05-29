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

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EditorController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class EditorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private GenreRepository genreRepository;

    @MockitoBean
    private PersonRepository personRepository;

    @MockitoBean
    private MovieCastRepository movieCastRepository;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private ArticleRepository articleRepository;

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testDashboard() throws Exception {
        when(movieService.findAll()).thenReturn(List.of());
        when(articleRepository.findByAuthorEmailOrderByCreatedAtDesc("editor@test.ru")).thenReturn(List.of());

        mockMvc.perform(get("/editor/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/dashboard"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("articles"));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testNewMovieForm() throws Exception {
        when(genreRepository.findAll()).thenReturn(List.of());
        when(personRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/editor/movies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/movie-form"))
                .andExpect(model().attributeExists("movie"))
                .andExpect(model().attributeExists("allGenres"))
                .andExpect(model().attributeExists("allPersons"));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testEditMovieForm_Success() throws Exception {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder().id(movieId).title("Начало").genres(Set.of()).build();

        when(movieService.findById(movieId)).thenReturn(Optional.of(movie));
        when(movieCastRepository.findByMovieId(movieId)).thenReturn(List.of());
        when(genreRepository.findAll()).thenReturn(List.of());
        when(personRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/editor/movies/" + movieId + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/movie-form"))
                .andExpect(model().attribute("movie", movie));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testEditMovieForm_NotFound() throws Exception {
        UUID movieId = UUID.randomUUID();
        when(movieService.findById(movieId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/editor/movies/" + movieId + "/edit"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testSaveMovie_New() throws Exception {
        Movie movie = Movie.builder().id(UUID.randomUUID()).title("Начало").build();
        when(movieService.save(any(Movie.class))).thenReturn(movie);

        mockMvc.perform(post("/editor/movies/save")
                .with(csrf())
                .param("title", "Начало")
                .param("description", "Сны во сне")
                .param("releaseYear", "2010")
                .param("durationMinutes", "148"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(movieService, times(1)).save(any(Movie.class));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testDeleteMovie() throws Exception {
        UUID movieId = UUID.randomUUID();
        when(movieCastRepository.findByMovieId(movieId)).thenReturn(List.of());

        mockMvc.perform(post("/editor/movies/" + movieId + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(movieCastRepository, times(1)).deleteAll(any());
        verify(movieService, times(1)).deleteById(movieId);
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testNewPersonForm() throws Exception {
        mockMvc.perform(get("/editor/persons/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/person-form"))
                .andExpect(model().attributeExists("person"));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testSavePerson() throws Exception {
        mockMvc.perform(post("/editor/persons/save")
                .with(csrf())
                .param("fullName", "Кристофер Нолан")
                .param("bio", "Режиссер")
                .param("birthDate", "1970-07-30")
                .param("photoUrl", "http://nolan.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(personRepository, times(1)).save(any(Person.class));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testReviewsList() throws Exception {
        when(reviewService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/editor/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/reviews"))
                .andExpect(model().attributeExists("reviews"));
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testApproveReview() throws Exception {
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(post("/editor/reviews/" + reviewId + "/approve")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/reviews"));

        verify(reviewService, times(1)).approveReview(reviewId);
    }

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testRejectReview() throws Exception {
        UUID reviewId = UUID.randomUUID();

        mockMvc.perform(post("/editor/reviews/" + reviewId + "/reject")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/reviews"));

        verify(reviewService, times(1)).rejectReview(reviewId);
    }
}
