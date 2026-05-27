package com.movieobserver.controller;

import com.movieobserver.domain.entity.*;
import com.movieobserver.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.movieobserver.service.MovieService;
import com.movieobserver.service.ReviewService;
import org.springframework.security.core.Authentication;

import java.util.*;

@Controller
@RequestMapping("/editor")
@RequiredArgsConstructor
public class EditorController {

    private final MovieService movieService;
    private final GenreRepository genreRepository;
    private final PersonRepository personRepository;
    private final MovieCastRepository movieCastRepository;
    private final ReviewService reviewService;
    private final ArticleRepository articleRepository;

    // ── Dashboard ───────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("movies", movieService.findAll());
        if (authentication != null) {
            model.addAttribute("articles", articleRepository.findByAuthorEmailOrderByCreatedAtDesc(authentication.getName()));
        } else {
            model.addAttribute("articles", List.of());
        }
        return "editor/dashboard";
    }

    // ── Movies ──────────────────────────────────────────────────────────────

    @GetMapping("/movies/new")
    public String newMovieForm(Model model) {
        model.addAttribute("movie", new Movie());
        model.addAttribute("allGenres", genreRepository.findAll());
        model.addAttribute("allPersons", personRepository.findAll());
        return "editor/movie-form";
    }

    @GetMapping("/movies/{id}/edit")
    public String editMovieForm(@PathVariable UUID id, Model model) {
        Movie movie = movieService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Фильм не найден"));

        List<MovieCast> cast = movieCastRepository.findByMovieId(id);
        List<UUID> directorIds = cast.stream()
                .filter(c -> "DIRECTOR".equalsIgnoreCase(c.getId().getRoleType()))
                .map(c -> c.getPerson().getId())
                .toList();
        List<UUID> actorIds = cast.stream()
                .filter(c -> "ACTOR".equalsIgnoreCase(c.getId().getRoleType()))
                .map(c -> c.getPerson().getId())
                .toList();
        List<Long> selectedGenreIds = movie.getGenres().stream()
                .map(Genre::getId)
                .toList();

        model.addAttribute("movie", movie);
        model.addAttribute("allGenres", genreRepository.findAll());
        model.addAttribute("allPersons", personRepository.findAll());
        model.addAttribute("selectedGenreIds", selectedGenreIds);
        model.addAttribute("selectedDirectorIds", directorIds);
        model.addAttribute("selectedActorIds", actorIds);
        return "editor/movie-form";
    }

    @PostMapping("/movies/save")
    public String saveMovie(
            @RequestParam(required = false) UUID id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam Integer releaseYear,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) String posterUrl,
            @RequestParam(value = "genreIds", required = false) List<Long> genreIds,
            @RequestParam(value = "directorIds", required = false) List<UUID> directorIds,
            @RequestParam(value = "actorIds", required = false) List<UUID> actorIds) {

        Movie movie;
        if (id != null) {
            movie = movieService.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        } else {
            movie = new Movie();
        }

        movie.setTitle(title);
        movie.setDescription(description);
        movie.setReleaseYear(releaseYear);
        movie.setDurationMinutes(durationMinutes);
        movie.setPosterUrl(posterUrl);

        // Update genres
        Set<Genre> genres = new HashSet<>();
        if (genreIds != null) {
            genres.addAll(genreRepository.findAllById(genreIds));
        }
        movie.setGenres(genres);

        movie = movieService.save(movie);

        // Update cast: delete old, insert new
        final UUID movieId = movie.getId();
        movieCastRepository.deleteAll(movieCastRepository.findByMovieId(movieId));

        List<MovieCast> newCast = new ArrayList<>();
        if (directorIds != null) {
            for (UUID personId : directorIds) {
                Person person = personRepository.findById(personId).orElse(null);
                if (person != null) {
                    MovieCastId castId = new MovieCastId(movieId, personId, "DIRECTOR");
                    newCast.add(MovieCast.builder().id(castId).movie(movie).person(person).build());
                }
            }
        }
        if (actorIds != null) {
            for (UUID personId : actorIds) {
                Person person = personRepository.findById(personId).orElse(null);
                if (person != null) {
                    MovieCastId castId = new MovieCastId(movieId, personId, "ACTOR");
                    newCast.add(MovieCast.builder().id(castId).movie(movie).person(person).build());
                }
            }
        }
        movieCastRepository.saveAll(newCast);

        return "redirect:/editor/dashboard";
    }

    @PostMapping("/movies/{id}/delete")
    public String deleteMovie(@PathVariable UUID id) {
        movieCastRepository.deleteAll(movieCastRepository.findByMovieId(id));
        movieService.deleteById(id);
        return "redirect:/editor/dashboard";
    }

    // ── Persons ─────────────────────────────────────────────────────────────

    @GetMapping("/persons/new")
    public String newPersonForm(Model model) {
        model.addAttribute("person", new Person());
        return "editor/person-form";
    }

    @PostMapping("/persons/save")
    public String savePerson(
            @RequestParam String fullName,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String birthDate,
            @RequestParam(required = false) String photoUrl) {

        Person person = new Person();
        person.setFullName(fullName);
        person.setBio(bio);
        person.setPhotoUrl(photoUrl);
        if (birthDate != null && !birthDate.isBlank()) {
            person.setBirthDate(java.time.LocalDate.parse(birthDate));
        }
        personRepository.save(person);
        return "redirect:/editor/dashboard";
    }

    // ── Reviews ─────────────────────────────────────────────────────────────

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", reviewService.findAll());
        return "editor/reviews";
    }

    @PostMapping("/reviews/{id}/approve")
    public String approveReview(@PathVariable UUID id) {
        reviewService.approveReview(id);
        return "redirect:/editor/reviews";
    }

    @PostMapping("/reviews/{id}/reject")
    public String rejectReview(@PathVariable UUID id) {
        reviewService.rejectReview(id);
        return "redirect:/editor/reviews";
    }
}
