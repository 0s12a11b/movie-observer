package com.movieobserver.controller;

import com.movieobserver.domain.entity.Article;
import com.movieobserver.domain.entity.Movie;
import com.movieobserver.domain.entity.User;
import com.movieobserver.repository.ArticleRepository;
import com.movieobserver.repository.MovieRepository;
import com.movieobserver.repository.UserRepository;
import com.movieobserver.security.JwtProvider;
import com.movieobserver.service.CustomUserDetailsService;
import com.movieobserver.config.SecurityConfig;
import com.movieobserver.security.JwtFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ArticleController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class ArticleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private ArticleRepository articleRepository;

    @MockitoBean
    private MovieRepository movieRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(username = "editor@test.ru", roles = {"EDITOR"})
    public void testNewArticleForm() throws Exception {
        when(movieRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/editor/articles/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/article-form"))
                .andExpect(model().attributeExists("article"))
                .andExpect(model().attributeExists("movies"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testEditArticleForm_Success() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .status(Article.ArticleStatus.DRAFT)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));
        when(movieRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/editor/articles/" + articleId + "/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/article-form"))
                .andExpect(model().attribute("article", article));
    }

    @Test
    @WithMockUser(username = "other@test.ru", roles = {"CRITIC"})
    public void testEditArticleForm_Forbidden() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .status(Article.ArticleStatus.DRAFT)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(get("/editor/articles/" + articleId + "/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testEditArticleForm_NotFound() throws Exception {
        UUID articleId = UUID.randomUUID();
        when(articleRepository.findById(articleId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/editor/articles/" + articleId + "/edit"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testSaveArticle_CreateSuccess() throws Exception {
        User author = User.builder().email("author@test.ru").build();
        when(userRepository.findByEmail("author@test.ru")).thenReturn(Optional.of(author));

        mockMvc.perform(post("/editor/articles/save")
                .with(csrf())
                .param("title", "Тестовый заголовок")
                .param("category", "REVIEW")
                .param("leadParagraph", "Введение статьи")
                .param("bodyContent", "Основное тело статьи")
                .param("status", "PUBLISHED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(articleRepository, times(1)).save(any(Article.class));
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testSaveArticle_ValidationError() throws Exception {
        mockMvc.perform(post("/editor/articles/save")
                .with(csrf())
                .param("title", "") // Empty title
                .param("category", "REVIEW")
                .param("leadParagraph", "Введение статьи")
                .param("bodyContent", "Основное тело статьи")
                .param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(view().name("editor/article-form"))
                .andExpect(model().attributeExists("errorMessage"));

        verify(articleRepository, never()).save(any(Article.class));
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testSaveArticle_UpdateSuccess() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(post("/editor/articles/save")
                .with(csrf())
                .param("id", articleId.toString())
                .param("title", "Обновленный заголовок")
                .param("category", "NEWS")
                .param("leadParagraph", "Новое введение")
                .param("bodyContent", "Новое тело статьи")
                .param("status", "DRAFT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(articleRepository, times(1)).save(article);
        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    @WithMockUser(username = "other@test.ru", roles = {"CRITIC"})
    public void testSaveArticle_UpdateForbidden() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(post("/editor/articles/save")
                .with(csrf())
                .param("id", articleId.toString())
                .param("title", "Обновленный заголовок")
                .param("category", "NEWS")
                .param("leadParagraph", "Новое введение")
                .param("bodyContent", "Новое тело статьи")
                .param("status", "DRAFT"))
                .andExpect(status().isForbidden());

        verify(articleRepository, never()).save(any());
    }

    @Test
    @WithMockUser(username = "author@test.ru", roles = {"CRITIC"})
    public void testDeleteArticle_Success() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(post("/editor/articles/" + articleId + "/delete")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/editor/dashboard"));

        verify(articleRepository, times(1)).delete(article);
    }

    @Test
    @WithMockUser(username = "other@test.ru", roles = {"CRITIC"})
    public void testDeleteArticle_Forbidden() throws Exception {
        UUID articleId = UUID.randomUUID();
        User author = User.builder().email("author@test.ru").build();
        Article article = Article.builder()
                .id(articleId)
                .author(author)
                .build();

        when(articleRepository.findById(articleId)).thenReturn(Optional.of(article));

        mockMvc.perform(post("/editor/articles/" + articleId + "/delete")
                .with(csrf()))
                .andExpect(status().isForbidden());

        verify(articleRepository, never()).delete(any());
    }
}
