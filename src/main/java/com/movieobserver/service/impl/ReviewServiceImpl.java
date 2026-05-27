package com.movieobserver.service.impl;

import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.Review;
import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.ReviewRepository;
import com.movieobserver.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final MovieRepository movieRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Review> findAll() {
        return reviewRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Review> findById(UUID id) {
        return reviewRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Review> findApprovedByMovieId(UUID movieId) {
        return reviewRepository.findByMovieIdAndIsApprovedTrueOrderByCreatedAtDesc(movieId);
    }

    @Override
    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    @Override
    public void delete(Review review) {
        reviewRepository.delete(review);
    }

    @Override
    public Review submitReview(UUID movieId, User user, Integer rating, String text) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));

        boolean isCritic = user.getRoles().stream()
                .anyMatch(role -> "ROLE_CRITIC".equalsIgnoreCase(role.getName()));

        Review review = Review.builder()
                .movie(movie)
                .user(user)
                .rating(rating)
                .reviewText(text)
                .isApproved(isCritic)
                .build();

        review = reviewRepository.save(review);

        if (isCritic) {
            recalculateMovieRating(movieId);
        }

        return review;
    }

    @Override
    public void approveReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рецензия не найдена"));
        review.setIsApproved(true);
        reviewRepository.save(review);
        recalculateMovieRating(review.getMovie().getId());
    }

    @Override
    public void rejectReview(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Рецензия не найдена"));
        UUID movieId = review.getMovie().getId();
        reviewRepository.delete(review);
        reviewRepository.flush();
        recalculateMovieRating(movieId);
    }

    @Override
    public void recalculateMovieRating(UUID movieId) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie != null) {
            Double avg = reviewRepository.getAverageRatingForMovie(movieId);
            movie.setAverageRating(avg != null ? BigDecimal.valueOf(avg) : BigDecimal.ZERO);
            movieRepository.save(movie);
        }
    }
}
