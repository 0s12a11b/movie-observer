package com.movieobserver.controller;

import com.movieobserver.domain.entity.Article;
import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.ArticleRepository;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Controller
@RequestMapping("/editor/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleRepository articleRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    @GetMapping("/new")
    public String newArticleForm(Model model) {
        model.addAttribute("article", new Article());
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("categories", Article.ArticleCategory.values());
        model.addAttribute("statuses", Article.ArticleStatus.values());
        return "editor/article-form";
    }

    @GetMapping("/{id}/edit")
    public String editArticleForm(@PathVariable UUID id, Model model, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));

        if (!article.getAuthor().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не являетесь автором этой статьи");
        }

        model.addAttribute("article", article);
        model.addAttribute("movies", movieRepository.findAll());
        model.addAttribute("categories", Article.ArticleCategory.values());
        model.addAttribute("statuses", Article.ArticleStatus.values());
        return "editor/article-form";
    }

    @PostMapping("/save")
    public String saveArticle(
            @RequestParam(required = false) UUID id,
            @RequestParam String title,
            @RequestParam String category,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) String bannerUrl,
            @RequestParam String leadParagraph,
            @RequestParam String bodyContent,
            @RequestParam String status,
            Authentication authentication,
            Model model) {

        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        String currentEmail = authentication.getName();

        // Server-side validation
        if (title == null || title.isBlank() ||
            leadParagraph == null || leadParagraph.isBlank() ||
            bodyContent == null || bodyContent.isBlank()) {
            
            Article article = id != null ? articleRepository.findById(id).orElse(new Article()) : new Article();
            article.setTitle(title);
            article.setLeadParagraph(leadParagraph);
            article.setBodyContent(bodyContent);
            article.setBannerUrl(bannerUrl);
            
            model.addAttribute("article", article);
            model.addAttribute("movies", movieRepository.findAll());
            model.addAttribute("categories", Article.ArticleCategory.values());
            model.addAttribute("statuses", Article.ArticleStatus.values());
            model.addAttribute("errorMessage", "Все обязательные поля (Заголовок, Введение, Текст) должны быть заполнены");
            
            return "editor/article-form";
        }

        Article article;
        if (id != null) {
            article = articleRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));
            if (!article.getAuthor().getEmail().equalsIgnoreCase(currentEmail)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не являетесь автором этой статьи");
            }
        } else {
            article = new Article();
            User author = userRepository.findByEmail(currentEmail)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
            article.setAuthor(author);
        }

        article.setTitle(title.trim());
        article.setCategory(Article.ArticleCategory.valueOf(category));
        article.setLeadParagraph(leadParagraph.trim());
        article.setBodyContent(bodyContent.trim());
        article.setBannerUrl(bannerUrl != null && !bannerUrl.isBlank() ? bannerUrl.trim() : null);
        article.setStatus(Article.ArticleStatus.valueOf(status));

        if (movieId != null) {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            article.setMovie(movie);
        } else {
            article.setMovie(null);
        }

        articleRepository.save(article);

        return "redirect:/editor/dashboard";
    }

    @PostMapping("/{id}/delete")
    public String deleteArticle(@PathVariable UUID id, Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));

        if (!article.getAuthor().getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Вы не являетесь автором этой статьи");
        }

        articleRepository.delete(article);
        return "redirect:/editor/dashboard";
    }
}
