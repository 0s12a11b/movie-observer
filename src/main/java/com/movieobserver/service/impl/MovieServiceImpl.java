package com.movieobserver.service.impl;

import com.movieobserver.domain.entity.Movie;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.service.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Movie> findAll() {
        return movieRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Movie> findById(UUID id) {
        return movieRepository.findById(id);
    }

    @Override
    public Movie save(Movie movie) {
        return movieRepository.save(movie);
    }

    @Override
    public void deleteById(UUID id) {
        movieRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Movie> searchMovies(String title, Long genreId) {
        return movieRepository.searchMovies(title, genreId);
    }
}
