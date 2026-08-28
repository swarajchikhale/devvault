package com.devvault.exception;

import com.devvault.auth.exception.DuplicateEmailException;
import com.devvault.auth.exception.DuplicateUsernameException;
import com.devvault.auth.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("Should handle DuplicateUsernameException with HTTP 409 and USERNAME_ALREADY_EXISTS")
    void handleDuplicateUsernameException() {
        DuplicateUsernameException ex = new DuplicateUsernameException("Username 'testuser' is already taken");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDuplicateUsernameException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("USERNAME_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("Username is already taken");
    }

    @Test
    @DisplayName("Should handle DuplicateEmailException with HTTP 409 and EMAIL_ALREADY_EXISTS")
    void handleDuplicateEmailException() {
        DuplicateEmailException ex = new DuplicateEmailException("Email 'test@example.com' is already registered");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleDuplicateEmailException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(409);
        assertThat(response.getBody().getError()).isEqualTo("EMAIL_ALREADY_EXISTS");
        assertThat(response.getBody().getMessage()).isEqualTo("Email is already registered");
    }

    @Test
    @DisplayName("Should handle InvalidCredentialsException with HTTP 401 and INVALID_CREDENTIALS")
    void handleInvalidCredentialsException() {
        InvalidCredentialsException ex = new InvalidCredentialsException("Invalid credentials");

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleInvalidCredentialsException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(401);
        assertThat(response.getBody().getError()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
    }

    @Test
    @DisplayName("Should handle MethodArgumentNotValidException with HTTP 400 and VALIDATION_ERROR")
    void handleMethodArgumentNotValidException() throws NoSuchMethodException {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        MethodParameter parameter = new MethodParameter(
                GlobalExceptionHandlerTest.class.getDeclaredMethod("setUp"), -1
        );
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ApiErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getError()).isEqualTo("VALIDATION_ERROR");
        assertThat(response.getBody().getMessage()).isEqualTo("Request validation failed");
    }
}
