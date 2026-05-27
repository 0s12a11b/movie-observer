package com.movieobserver.controller;

import com.movieobserver.domain.entity.Article;
import com.movieobserver.domain.entity.Genre;
import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.MovieCast;
import com.movieobserver.domain.entity.Review;
import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.ArticleRepository;
import com.movieobserver.repository.GenreRepository;
import com.movieobserver.repository.MovieCastRepository;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.ReviewRepository;
import com.movieobserver.repository.UserRepository;
import com.movieobserver.service.MovieService;
import com.movieobserver.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class WebCatalogController {

    private final MovieService movieService;
    private final GenreRepository genreRepository;
    private final ArticleRepository articleRepository;
    private final ReviewService reviewService;
    private final MovieCastRepository movieCastRepository;
    private final UserRepository userRepository;

    @GetMapping("/")
    public String homepage(Model model) {
        List<Article> articles = articleRepository.findByStatusOrderByCreatedAtDesc(Article.ArticleStatus.PUBLISHED);
        // Trending movies: top rated or simply all movies sorted by rating/title
        List<Movie> trendingMovies = movieService.findAll().stream()
                .sorted((m1, m2) -> m2.getAverageRating().compareTo(m1.getAverageRating()))
                .limit(5)
                .toList();

        model.addAttribute("articles", articles);
        model.addAttribute("trendingMovies", trendingMovies);
        return "index";
    }

    @GetMapping("/movies")
    public String moviesList(
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "genreId", required = false) Long genreId,
            Model model) {

        List<Movie> movies = movieService.searchMovies(title, genreId);
        List<Genre> genres = genreRepository.findAll();

        model.addAttribute("movies", movies);
        model.addAttribute("genres", genres);
        model.addAttribute("searchTitle", title);
        model.addAttribute("selectedGenreId", genreId);
        return "movies/list";
    }

    @GetMapping("/movies/{id}")
    public String movieDetail(@PathVariable("id") UUID id, Model model) {
        Movie movie = movieService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));

        List<MovieCast> cast = movieCastRepository.findByMovieId(id);
        List<MovieCast> directors = cast.stream()
                .filter(c -> "DIRECTOR".equalsIgnoreCase(c.getId().getRoleType()))
                .toList();
        List<MovieCast> actors = cast.stream()
                .filter(c -> "ACTOR".equalsIgnoreCase(c.getId().getRoleType()))
                .toList();

        List<Review> reviews = reviewService.findApprovedByMovieId(id);

        model.addAttribute("movie", movie);
        model.addAttribute("directors", directors);
        model.addAttribute("actors", actors);
        model.addAttribute("reviews", reviews);
        return "movies/detail";
    }

    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable("id") UUID id, Model model) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не найдена"));

        if (article.getStatus() != Article.ArticleStatus.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Статья не опубликована");
        }

        model.addAttribute("article", article);
        return "articles/detail";
    }

    @PostMapping("/movies/{id}/reviews")
    public String addReview(
            @PathVariable("id") UUID id,
            @RequestParam("rating") Integer rating,
            @RequestParam("reviewText") String reviewText,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Вы должны войти в систему");
        }

        Movie movie = movieService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Пользователь не найден"));

        // Validation
        if (rating == null || rating < 1 || rating > 10) {
            redirectAttributes.addFlashAttribute("errorMessage", "Оценка должна быть от 1 до 10");
            return "redirect:/movies/" + id;
        }
        if (reviewText == null || reviewText.trim().length() < 10) {
            redirectAttributes.addFlashAttribute("errorMessage", "Текст рецензии должен содержать не менее 10 символов");
            return "redirect:/movies/" + id;
        }

        Review review = reviewService.submitReview(id, user, rating, reviewText.trim());

        if (review.getIsApproved()) {
            redirectAttributes.addFlashAttribute("successMessage", "Ваша рецензия успешно опубликована!");
        } else {
            redirectAttributes.addFlashAttribute("successMessage", "Ваш отзыв успешно отправлен и будет опубликован после проверки модератором.");
        }
        return "redirect:/movies/" + id;
    }
}
