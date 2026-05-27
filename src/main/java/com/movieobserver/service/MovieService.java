package com.movieobserver.service;

import com.movieobserver.domain.entity.Movie;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MovieService {
    List<Movie> findAll();
    Optional<Movie> findById(UUID id);
    Movie save(Movie movie);
    void deleteById(UUID id);
    List<Movie> searchMovies(String title, Long genreId);
}
