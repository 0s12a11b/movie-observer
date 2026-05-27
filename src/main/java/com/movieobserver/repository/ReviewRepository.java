package com.movieobserver.repository;

import com.movieobserver.domain.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    List<Review> findByMovieIdAndIsApprovedTrueOrderByCreatedAtDesc(UUID movieId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId AND r.isApproved = true")
    Double getAverageRatingForMovie(@Param("movieId") UUID movieId);
}
