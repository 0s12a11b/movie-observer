package com.movieobserver.service;

import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.Review;
import com.movieobserver.domain.entity.Role;
import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.ReviewRepository;
import com.movieobserver.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MovieRepository movieRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Movie testMovie;
    private User testUser;
    private Role userRole;
    private Role criticRole;
    private UUID movieId;
    private UUID reviewId;

    @BeforeEach
    public void setUp() {
        movieId = UUID.randomUUID();
        reviewId = UUID.randomUUID();

        testMovie = Movie.builder()
                .id(movieId)
                .title("Начало")
                .description("Фильм Нолана")
                .releaseYear(2010)
                .durationMinutes(148)
                .averageRating(BigDecimal.ZERO)
                .build();

        userRole = Role.builder().id(1L).name("ROLE_USER").build();
        criticRole = Role.builder().id(2L).name("ROLE_CRITIC").build();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@test.ru")
                .displayName("Глеб")
                .roles(new HashSet<>(List.of(userRole)))
                .build();
    }

    @Test
    public void testFindAll() {
        Review review = Review.builder().id(reviewId).build();
        when(reviewRepository.findAll()).thenReturn(List.of(review));

        List<Review> result = reviewService.findAll();
        assertEquals(1, result.size());
        verify(reviewRepository, times(1)).findAll();
    }

    @Test
    public void testFindById() {
        Review review = Review.builder().id(reviewId).build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

        Optional<Review> result = reviewService.findById(reviewId);
        assertTrue(result.isPresent());
        assertEquals(reviewId, result.get().getId());
    }

    @Test
    public void testFindApprovedByMovieId() {
        Review review = Review.builder().id(reviewId).isApproved(true).build();
        when(reviewRepository.findByMovieIdAndIsApprovedTrueOrderByCreatedAtDesc(movieId)).thenReturn(List.of(review));

        List<Review> result = reviewService.findApprovedByMovieId(movieId);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsApproved());
    }

    @Test
    public void testSubmitReview_RegularUser() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        
        Review savedReview = Review.builder()
                .movie(testMovie)
                .user(testUser)
                .rating(8)
                .reviewText("Хороший фильм")
                .isApproved(false)
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

        Review result = reviewService.submitReview(movieId, testUser, 8, "Хороший фильм");

        assertNotNull(result);
        assertFalse(result.getIsApproved());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(reviewRepository, never()).getAverageRatingForMovie(movieId);
    }

    @Test
    public void testSubmitReview_Critic() {
        testUser.getRoles().add(criticRole);
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));

        Review savedReview = Review.builder()
                .movie(testMovie)
                .user(testUser)
                .rating(10)
                .reviewText("Шедевр")
                .isApproved(true)
                .build();
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.getAverageRatingForMovie(movieId)).thenReturn(10.0);

        Review result = reviewService.submitReview(movieId, testUser, 10, "Шедевр");

        assertNotNull(result);
        assertTrue(result.getIsApproved());
        verify(reviewRepository, times(1)).save(any(Review.class));
        verify(movieRepository, times(1)).save(testMovie);
        assertEquals(BigDecimal.valueOf(10.0), testMovie.getAverageRating());
    }

    @Test
    public void testSubmitReview_MovieNotFound() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> {
            reviewService.submitReview(movieId, testUser, 8, "Норм");
        });
    }

    @Test
    public void testApproveReview() {
        Review review = Review.builder()
                .id(reviewId)
                .movie(testMovie)
                .isApproved(false)
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        when(reviewRepository.getAverageRatingForMovie(movieId)).thenReturn(9.5);

        reviewService.approveReview(reviewId);

        assertTrue(review.getIsApproved());
        verify(reviewRepository, times(1)).save(review);
        verify(movieRepository, times(1)).save(testMovie);
        assertEquals(BigDecimal.valueOf(9.5), testMovie.getAverageRating());
    }

    @Test
    public void testRejectReview() {
        Review review = Review.builder()
                .id(reviewId)
                .movie(testMovie)
                .isApproved(true)
                .build();
        when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        when(reviewRepository.getAverageRatingForMovie(movieId)).thenReturn(null);

        reviewService.rejectReview(reviewId);

        verify(reviewRepository, times(1)).delete(review);
        verify(reviewRepository, times(1)).flush();
        verify(movieRepository, times(1)).save(testMovie);
        assertEquals(BigDecimal.ZERO, testMovie.getAverageRating());
    }

    @Test
    public void testRecalculateMovieRating_NullAvg() {
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(testMovie));
        when(reviewRepository.getAverageRatingForMovie(movieId)).thenReturn(null);

        reviewService.recalculateMovieRating(movieId);

        assertEquals(BigDecimal.ZERO, testMovie.getAverageRating());
        verify(movieRepository, times(1)).save(testMovie);
    }
}
