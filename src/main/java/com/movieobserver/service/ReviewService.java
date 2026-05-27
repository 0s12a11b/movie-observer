package com.movieobserver.service;

import com.movieobserver.domain.entity.Review;
import com.movieobserver.domain.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewService {
    List<Review> findAll();
    Optional<Review> findById(UUID id);
    List<Review> findApprovedByMovieId(UUID movieId);
    Review save(Review review);
    void delete(Review review);
    Review submitReview(UUID movieId, User user, Integer rating, String text);
    void approveReview(UUID reviewId);
    void rejectReview(UUID reviewId);
    void recalculateMovieRating(UUID movieId);
}
