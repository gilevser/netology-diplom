package netology.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.netology.controller.AuthController;
import ru.netology.service.AuthService;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private final String testLogin = "test@example.com";
    private final String testPassword = "password123";
    private final String testToken = "jwt-token-123";

//    @BeforeEach
//    void setUp() {
//
//    }

    @Test
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        // Arrange
        when(authService.authenticate(testLogin, testPassword))
                .thenReturn(Optional.of(testToken));

        Map<String, String> authRequest = Map.of(
                "login", testLogin,
                "password", testPassword
        );

        ResponseEntity<?> response = authController.login(authRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals(testToken, responseBody.get("auth-token"));

        verify(authService, times(1)).authenticate(testLogin, testPassword);
    }

    @Test
    void login_ShouldReturnBadRequest_WhenCredentialsAreInvalid() {
        when(authService.authenticate(testLogin, testPassword))
                .thenReturn(Optional.empty());

        Map<String, String> authRequest = Map.of(
                "login", testLogin,
                "password", testPassword
        );

        ResponseEntity<?> response = authController.login(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Bad credentials", responseBody.get("message"));

        verify(authService, times(1)).authenticate(testLogin, testPassword);
    }

    @Test
    void login_ShouldReturnBadRequest_WhenLoginIsMissing() {
        Map<String, String> authRequest = Map.of(
                "password", testPassword
        );

        ResponseEntity<?> response = authController.login(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Bad credentials", responseBody.get("message"));

        verify(authService, never()).authenticate(any(), any());
    }

    @Test
    void login_ShouldReturnBadRequest_WhenPasswordIsMissing() {
        Map<String, String> authRequest = Map.of(
                "login", testLogin
        );

        ResponseEntity<?> response = authController.login(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Bad credentials", responseBody.get("message"));

        verify(authService, never()).authenticate(any(), any());
    }

    @Test
    void register_ShouldReturnSuccess_WhenRegistrationIsSuccessful() {
        when(authService.register(testLogin, testPassword))
                .thenReturn(true);

        Map<String, String> authRequest = Map.of(
                "login", testLogin,
                "password", testPassword
        );

        ResponseEntity<?> response = authController.register(authRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("User registered successfully", responseBody.get("message"));

        verify(authService, times(1)).register(testLogin, testPassword);
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUserAlreadyExists() {
        when(authService.register(testLogin, testPassword))
                .thenReturn(false);

        Map<String, String> authRequest = Map.of(
                "login", testLogin,
                "password", testPassword
        );

        ResponseEntity<?> response = authController.register(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("User already exists", responseBody.get("message"));

        verify(authService, times(1)).register(testLogin, testPassword);
    }

    @Test
    void register_ShouldReturnBadRequest_WhenLoginIsNull() {
        Map<String, String> authRequest = Map.of(
                "password", testPassword
        );

        ResponseEntity<?> response = authController.register(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Login and password required", responseBody.get("message"));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenPasswordIsNull() {
        Map<String, String> authRequest = Map.of(
                "login", testLogin
        );

        ResponseEntity<?> response = authController.register(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Login and password required", responseBody.get("message"));

        verify(authService, never()).register(any(), any());
    }

    @Test
    void register_ShouldReturnBadRequest_WhenBothCredentialsAreMissing() {
        Map<String, String> authRequest = Map.of();

        ResponseEntity<?> response = authController.register(authRequest);

        assertNotNull(response);
        assertEquals(400, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, String> responseBody = (Map<String, String>) response.getBody();
        assertEquals("Login and password required", responseBody.get("message"));

        verify(authService, never()).register(any(), any());
    }
}