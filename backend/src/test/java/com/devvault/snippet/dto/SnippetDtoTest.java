package com.devvault.snippet.dto;

import com.devvault.snippet.Snippet;
import com.devvault.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SnippetDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CreateSnippetRequest with valid data should produce no violations")
    void createSnippetRequest_ValidData_NoViolations() {
        CreateSnippetRequest request = new CreateSnippetRequest(
                "Read File in Java",
                "Read the contents of a file",
                "Files.readString(Path.of(\"file.txt\"));",
                "java"
        );
        Set<ConstraintViolation<CreateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateSnippetRequest with blank required fields should produce violations")
    void createSnippetRequest_BlankFields_HasViolations() {
        CreateSnippetRequest request = new CreateSnippetRequest("", null, "", "");
        Set<ConstraintViolation<CreateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(3);
    }

    @Test
    @DisplayName("CreateSnippetRequest with title exceeding 200 chars should produce violation")
    void createSnippetRequest_TitleTooLong_HasViolation() {
        String longTitle = "a".repeat(201);
        CreateSnippetRequest request = new CreateSnippetRequest(longTitle, "desc", "code", "java");
        Set<ConstraintViolation<CreateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("CreateSnippetRequest with language exceeding 50 chars should produce violation")
    void createSnippetRequest_LanguageTooLong_HasViolation() {
        String longLang = "a".repeat(51);
        CreateSnippetRequest request = new CreateSnippetRequest("title", "desc", "code", longLang);
        Set<ConstraintViolation<CreateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("UpdateSnippetRequest with valid data should produce no violations")
    void updateSnippetRequest_ValidData_NoViolations() {
        UpdateSnippetRequest request = new UpdateSnippetRequest(
                "Updated Title",
                "Updated Description",
                "console.log('hello');",
                "javascript"
        );
        Set<ConstraintViolation<UpdateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateSnippetRequest with blank code should produce violation")
    void updateSnippetRequest_BlankCode_HasViolation() {
        UpdateSnippetRequest request = new UpdateSnippetRequest("Title", null, "   ", "python");
        Set<ConstraintViolation<UpdateSnippetRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("SnippetResponse.fromEntity should correctly map Snippet fields")
    void snippetResponse_FromEntity_CorrectMapping() {
        User user = new User("johndoe", "john@example.com", "hash");
        Snippet snippet = new Snippet(user, "My Snippet", "Description", "SELECT 1;", "sql");

        SnippetResponse response = SnippetResponse.fromEntity(snippet);

        assertThat(response.getTitle()).isEqualTo("My Snippet");
        assertThat(response.getDescription()).isEqualTo("Description");
        assertThat(response.getCode()).isEqualTo("SELECT 1;");
        assertThat(response.getLanguage()).isEqualTo("sql");
    }
}
