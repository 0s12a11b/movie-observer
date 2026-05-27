package com.movieobserver.repository;

import com.movieobserver.domain.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import com.movieobserver.domain.entity.Article;
import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, UUID> {
    List<Article> findByStatusOrderByCreatedAtDesc(Article.ArticleStatus status);
    List<Article> findByAuthorEmailOrderByCreatedAtDesc(String email);
}
