package com.movieobserver.service;

import com.movieobserver.domain.entity.Movie;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.service.impl.MovieServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MovieServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private MovieServiceImpl movieService;

    private Movie testMovie;
    private UUID movieId;

    @BeforeEach
    public void setUp() {
        movieId = UUID.randomUUID();
        testMovie = Movie.builder()
                .id(movieId)
                .title("Интерстеллар")
                .description("Космическая одиссея Нолана")
                .releaseYear(2014)
                .durationMinutes(169)
                .averageRating(BigDecimal.valueOf(9.0))
                .createdAt(OffsetDateTime.now())
                .build();
    }

    @Test
    public void testFindAll() {
        when(movieRepository.findAll()).thenReturn(List.of(testMovie));
        List<Movie> result = movieService.findAll();
        assertEquals(1, result.size());
        assertEquals("Интерстеллар", result.get(0).getTitle());
        verify(movieRepository, times(1)).findAll();
    }

    @Test
    public void testFindById_Found() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        Optional<Movie> result = movieService.findById(movieId);
        assertTrue(result.isPresent());
        assertEquals("Интерстеллар", result.get().getTitle());
        verify(movieRepository, times(1)).findById(movieId);
    }

    @Test
    public void testFindById_NotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());
        Optional<Movie> result = movieService.findById(movieId);
        assertFalse(result.isPresent());
        verify(movieRepository, times(1)).findById(movieId);
    }

    @Test
    public void testSave() {
        when(movieRepository.save(testMovie)).thenReturn(testMovie);
        Movie result = movieService.save(testMovie);
        assertNotNull(result);
        assertEquals("Интерстеллар", result.getTitle());
        verify(movieRepository, times(1)).save(testMovie);
    }

    @Test
    public void testDeleteById() {
        doNothing().when(movieRepository).deleteById(movieId);
        movieService.deleteById(movieId);
        verify(movieRepository, times(1)).deleteById(movieId);
    }

    @Test
    public void testSearchMovies() {
        when(movieRepository.searchMovies("Интер", 1L)).thenReturn(List.of(testMovie));
        List<Movie> result = movieService.searchMovies("Интер", 1L);
        assertEquals(1, result.size());
        assertEquals("Интерстеллар", result.get(0).getTitle());
        verify(movieRepository, times(1)).searchMovies("Интер", 1L);
    }
}
