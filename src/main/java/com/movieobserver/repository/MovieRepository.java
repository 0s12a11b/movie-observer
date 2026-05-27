package com.movieobserver.repository;

import com.movieobserver.domain.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {

    @Query("SELECT DISTINCT m FROM Movie m LEFT JOIN m.genres g WHERE " +
           "(:title IS NULL OR :title = '' OR LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%'))) AND " +
           "(:genreId IS NULL OR g.id = :genreId) " +
           "ORDER BY m.title ASC")
    List<Movie> searchMovies(@Param("title") String title, @Param("genreId") Long genreId);
}
