package com.movieobserver.controller;

import com.movieobserver.domain.entity.Role;
import com.movieobserver.domain.entity.User;
import com.movieobserver.dto.RegisterDto;
import com.movieobserver.repository.RoleRepository;
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
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtFilter.class})
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    public void testLoginPage_WithoutError() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeDoesNotExist("loginError"));
    }

    @Test
    public void testLoginPage_WithError() throws Exception {
        mockMvc.perform(get("/login").param("error", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginError"));
    }

    @Test
    public void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerDto"));
    }

    @Test
    public void testRegister_Success() throws Exception {
        Role mockRole = Role.builder().id(1L).name("ROLE_USER").build();
        when(userRepository.findByEmail("test@test.ru")).thenReturn(Optional.empty());
        when(roleRepository.findById(1L)).thenReturn(Optional.of(mockRole));
        when(passwordEncoder.encode("password")).thenReturn("hashedPassword");

        mockMvc.perform(post("/register")
                .param("email", "test@test.ru")
                .param("password", "password")
                .param("displayName", "Test User"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testRegister_ValidationError() throws Exception {
        mockMvc.perform(post("/register")
                .param("email", "")
                .param("password", "password")
                .param("displayName", "Test User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testRegister_AlreadyExists() throws Exception {
        User existingUser = User.builder().email("test@test.ru").build();
        when(userRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/register")
                .param("email", "test@test.ru")
                .param("password", "password")
                .param("displayName", "Test User"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void testLogin_Success() throws Exception {
        User user = User.builder()
                .email("test@test.ru")
                .passwordHash("hashedPassword")
                .build();
        when(userRepository.findByEmail("test@test.ru")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "hashedPassword")).thenReturn(true);
        when(jwtProvider.generateToken("test@test.ru")).thenReturn("mock-jwt-token");
        
        ResponseCookie cookie = ResponseCookie.from("JWT_TOKEN", "mock-jwt-token").build();
        when(jwtProvider.createCookie("mock-jwt-token")).thenReturn(cookie);

        mockMvc.perform(post("/login")
                .param("email", "test@test.ru")
                .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testLogin_Failure() throws Exception {
        when(userRepository.findByEmail("test@test.ru")).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                .param("email", "test@test.ru")
                .param("password", "wrong"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    public void testLogoutGet() throws Exception {
        ResponseCookie deleteCookie = ResponseCookie.from("JWT_TOKEN", "").maxAge(0).build();
        when(jwtProvider.createDeleteCookie()).thenReturn(deleteCookie);

        mockMvc.perform(get("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    public void testLogoutPost() throws Exception {
        ResponseCookie deleteCookie = ResponseCookie.from("JWT_TOKEN", "").maxAge(0).build();
        when(jwtProvider.createDeleteCookie()).thenReturn(deleteCookie);

        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
