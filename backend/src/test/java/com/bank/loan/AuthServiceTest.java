package com.bank.loan;

import com.bank.loan.config.JwtConfig;
import com.bank.loan.dto.LoginRequest;
import com.bank.loan.dto.LoginResponse;
import com.bank.loan.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtConfig jwtConfig;

    @Test
    void login_whenUserExistsAndPasswordCorrect_shouldReturnTokenAndUserInfo() {
        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("123456");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
        assertNotNull(response.getUserInfo());
        assertEquals(1L, response.getUserInfo().getId());
        assertEquals("customer1", response.getUserInfo().getUsername());
        assertEquals("customer", response.getUserInfo().getRole());
    }

    @Test
    void login_whenUserNotExists_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setUsername("not_exists");
        request.setPassword("123456");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void login_whenPasswordWrong_shouldThrow() {
        LoginRequest request = new LoginRequest();
        request.setUsername("customer1");
        request.setPassword("wrong_password");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));
        assertEquals("用户名或密码错误", ex.getMessage());
    }

    @Test
    void generateToken_shouldContainClaims() {
        String token = jwtConfig.generateToken(1L, "customer1", "customer");

        Claims claims = jwtConfig.parseToken(token);
        assertEquals(1L, claims.get("userId", Long.class));
        assertEquals("customer1", claims.get("username", String.class));
        assertEquals("customer", claims.get("role", String.class));
        assertEquals("customer1", claims.getSubject());
    }

    @Test
    void validateToken_whenExpired_shouldReturnFalse() throws Exception {
        SecretKey key = getJwtKey();
        String expiredToken = Jwts.builder()
                .setSubject("customer1")
                .claim("userId", 1L)
                .claim("username", "customer1")
                .claim("role", "customer")
                .setIssuedAt(new Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000 - 1000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(jwtConfig.validateToken(expiredToken));
    }

    @Test
    void validateToken_whenInvalid_shouldReturnFalse() {
        assertFalse(jwtConfig.validateToken("invalid.token.value"));
        assertFalse(jwtConfig.validateToken(""));
    }

    private SecretKey getJwtKey() throws Exception {
        Field field = JwtConfig.class.getDeclaredField("key");
        field.setAccessible(true);
        return (SecretKey) field.get(jwtConfig);
    }
}
