package com.devvault.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Valid LoginRequest should pass validation")
    void validLoginRequest_ShouldPassValidation() {
        LoginRequest request = new LoginRequest("johndoe", "SecurePass123");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Blank identifier and password should trigger validation errors")
    void blankLoginRequest_ShouldFailValidation() {
        LoginRequest request = new LoginRequest("", "  ");
        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(2);
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("identifier"));
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("LoginRequest toString should mask plaintext password")
    void loginRequest_ToString_ShouldMaskPassword() {
        LoginRequest request = new LoginRequest("johndoe", "SecretPassword");
        String str = request.toString();
        assertThat(str).doesNotContain("SecretPassword");
        assertThat(str).contains("[PROTECTED]");
    }

    @Test
    @DisplayName("LoginResponse should correctly hold tokens and protect in toString")
    void loginResponse_ShouldHoldData() {
        LoginResponse response1 = new LoginResponse("sample.jwt.token", "Bearer");
        LoginResponse response2 = new LoginResponse("sample.jwt.token", "Bearer");

        assertThat(response1.getAccessToken()).isEqualTo("sample.jwt.token");
        assertThat(response1.getTokenType()).isEqualTo("Bearer");
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
        assertThat(response1.toString()).doesNotContain("sample.jwt.token");
        assertThat(response1.toString()).contains("[PROTECTED]");
    }
}
