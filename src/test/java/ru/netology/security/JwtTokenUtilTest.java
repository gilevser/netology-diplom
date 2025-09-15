//package ru.netology.security;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.security.core.userdetails.User;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//class JwtTokenUtilTest {
//
//    private JwtTokenUtil jwtTokenUtil;
//    private UserDetails userDetails;
//
//    @BeforeEach
//    void setUp() {
//        jwtTokenUtil = new JwtTokenUtil();
//
//        userDetails = new User("testUser", "password", List.of());
//    }
//
//    @Test
//    void generateToken_ShouldReturnValidToken() {
//        // Act
//        String token = jwtTokenUtil.generateToken(userDetails.getUsername());
//
//        // Assert
//        assertNotNull(token);
//        assertTrue(token.length() > 0);
//    }
//
//    @Test
//    void getUsernameFromToken_ShouldReturnUsername() {
//        // Arrange
//        String token = jwtTokenUtil.generateToken(userDetails.getUsername());
//
//        // Act
//        String username = jwtTokenUtil.getUsernameFromToken(token);
//
//        // Assert
//        assertEquals("testuser", username);
//    }
//}