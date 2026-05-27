package com.movieobserver.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    public void setUp() {
        jwtProvider = new JwtProvider();
        // Set values using ReflectionTestUtils (mimicking Spring @Value injection)
        ReflectionTestUtils.setField(jwtProvider, "secret", "my-super-secret-key-that-is-long-enough-for-hmac-sha-256-signature-verification");
        ReflectionTestUtils.setField(jwtProvider, "expirationMs", 3600000L); // 1 hour
        jwtProvider.init();
    }

    @Test
    public void testGenerateAndValidateToken() {
        String email = "test@movieobserver.ru";
        String token = jwtProvider.generateToken(email);
        
        assertNotNull(token);
        assertTrue(jwtProvider.validateToken(token));
        assertEquals(email, jwtProvider.getUsernameFromToken(token));
    }

    @Test
    public void testValidateToken_InvalidSignature() {
        String token = "invalid.token.here";
        assertFalse(jwtProvider.validateToken(token));
    }

    @Test
    public void testResolveToken_CookiePresent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[]{
                new Cookie("OTHER_COOKIE", "value"),
                new Cookie("JWT_TOKEN", "my-jwt-token-value")
        };
        when(request.getCookies()).thenReturn(cookies);

        String result = jwtProvider.resolveToken(request);
        assertEquals("my-jwt-token-value", result);
    }

    @Test
    public void testResolveToken_CookieAbsent() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = new Cookie[]{
                new Cookie("OTHER_COOKIE", "value")
        };
        when(request.getCookies()).thenReturn(cookies);

        String result = jwtProvider.resolveToken(request);
        assertNull(result);
    }

    @Test
    public void testResolveToken_NoCookies() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        String result = jwtProvider.resolveToken(request);
        assertNull(result);
    }

    @Test
    public void testCreateCookie() {
        ResponseCookie cookie = jwtProvider.createCookie("my-token");
        assertNotNull(cookie);
        assertEquals("JWT_TOKEN", cookie.getName());
        assertEquals("my-token", cookie.getValue());
        assertTrue(cookie.isHttpOnly());
        assertEquals("/", cookie.getPath());
        assertEquals("Strict", cookie.getSameSite());
    }

    @Test
    public void testCreateDeleteCookie() {
        ResponseCookie cookie = jwtProvider.createDeleteCookie();
        assertNotNull(cookie);
        assertEquals("JWT_TOKEN", cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(0, cookie.getMaxAge().getSeconds());
    }
}
