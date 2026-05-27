package com.movieobserver.repository;

import com.movieobserver.domain.entity.MovieCast;
import com.movieobserver.domain.entity.MovieCastId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MovieCastRepository extends JpaRepository<MovieCast, MovieCastId> {
    List<MovieCast> findByMovieId(UUID movieId);
}
