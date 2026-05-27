package com.movieobserver.controller;

import com.movieobserver.domain.entity.Role;
import com.movieobserver.domain.entity.User;
import com.movieobserver.dto.RegisterDto;
import com.movieobserver.repository.RoleRepository;
import com.movieobserver.repository.UserRepository;
import com.movieobserver.security.JwtProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Неверный адрес электронной почты или пароль");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDto", new RegisterDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute RegisterDto registerDto, Model model) {
        if (registerDto.getEmail() == null || registerDto.getEmail().isBlank() ||
            registerDto.getPassword() == null || registerDto.getPassword().isBlank() ||
            registerDto.getDisplayName() == null || registerDto.getDisplayName().isBlank()) {
            model.addAttribute("error", "Все поля обязательны для заполнения");
            return "auth/register";
        }

        if (userRepository.findByEmail(registerDto.getEmail()).isPresent()) {
            model.addAttribute("error", "Пользователь с таким email уже зарегистрирован");
            return "auth/register";
        }

        Role userRole = roleRepository.findById(1L)
                .orElseGet(() -> roleRepository.findByName("ROLE_USER")
                        .orElseThrow(() -> new RuntimeException("Default role ROLE_USER not found")));

        User user = User.builder()
                .email(registerDto.getEmail())
                .passwordHash(passwordEncoder.encode(registerDto.getPassword()))
                .displayName(registerDto.getDisplayName())
                .roles(Set.of(userRole))
                .build();

        userRepository.save(user);
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpServletResponse response) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPasswordHash())) {
            String token = jwtProvider.generateToken(email);
            ResponseCookie cookie = jwtProvider.createCookie(token);
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
            return "redirect:/";
        }
        return "redirect:/login?error=true";
    }

    @GetMapping("/logout")
    public String logoutGet(HttpServletResponse response) {
        ResponseCookie cookie = jwtProvider.createDeleteCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }

    @PostMapping("/logout")
    public String logoutPost(HttpServletResponse response) {
        ResponseCookie cookie = jwtProvider.createDeleteCookie();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return "redirect:/";
    }
}
