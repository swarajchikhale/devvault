package com.devvault.resource.dto;

import com.devvault.resource.Resource;
import com.devvault.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "http://localhost:8080/api/resources",
            "https://sub.domain.org/path/to/resource?query=123#fragment"
    })
    @DisplayName("CreateResourceRequest should accept valid HTTP and HTTPS URLs")
    void createResourceRequest_ValidHttpAndHttpsUrls_NoViolations(String url) {
        CreateResourceRequest request = new CreateResourceRequest("Spring Boot Docs", url, "Official documentation", "Java");
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file.txt",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "mailto:user@example.com",
            "not-a-valid-url",
            "http://",
            "https://"
    })
    @DisplayName("CreateResourceRequest should reject non-HTTP/HTTPS schemes and malformed URLs")
    void createResourceRequest_NonHttpOrMalformedUrls_HasViolation(String url) {
        CreateResourceRequest request = new CreateResourceRequest("Spring Docs", url, "Docs", "Java");
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");
    }

    @Test
    @DisplayName("CreateResourceRequest with blank required fields should produce violations")
    void createResourceRequest_BlankFields_HasViolations() {
        CreateResourceRequest request = new CreateResourceRequest("", "", null, null);
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).extracting(v -> v.getPropertyPath().toString()).contains("title", "url");
    }

    @Test
    @DisplayName("CreateResourceRequest with title exceeding 200 chars should produce violation")
    void createResourceRequest_TitleTooLong_HasViolation() {
        String longTitle = "a".repeat(201);
        CreateResourceRequest request = new CreateResourceRequest(
                longTitle,
                "https://example.com",
                "desc",
                "Java"
        );
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("CreateResourceRequest with URL exceeding 2048 chars should produce violation")
    void createResourceRequest_UrlTooLong_HasViolation() {
        String longUrl = "https://example.com/" + "a".repeat(2040);
        CreateResourceRequest request = new CreateResourceRequest(
                "Title",
                longUrl,
                "desc",
                "Java"
        );
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");
    }

    @Test
    @DisplayName("CreateResourceRequest with category exceeding 100 chars should produce violation")
    void createResourceRequest_CategoryTooLong_HasViolation() {
        String longCategory = "a".repeat(101);
        CreateResourceRequest request = new CreateResourceRequest(
                "Title",
                "https://example.com",
                "desc",
                longCategory
        );
        Set<ConstraintViolation<CreateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://updated-url.org",
            "https://updated-url.org/api"
    })
    @DisplayName("UpdateResourceRequest should accept valid HTTP and HTTPS URLs")
    void updateResourceRequest_ValidHttpAndHttpsUrls_NoViolations(String url) {
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Title", url, "Updated description", "Backend");
        Set<ConstraintViolation<UpdateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file.txt",
            "file:///etc/passwd",
            "javascript:alert(1)",
            "not-a-valid-url"
    })
    @DisplayName("UpdateResourceRequest should reject non-HTTP/HTTPS schemes and malformed URLs")
    void updateResourceRequest_NonHttpOrMalformedUrls_HasViolation(String url) {
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Title", url, "Updated description", "Backend");
        Set<ConstraintViolation<UpdateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("url");
    }

    @Test
    @DisplayName("UpdateResourceRequest with blank title should produce violation")
    void updateResourceRequest_BlankTitle_HasViolation() {
        UpdateResourceRequest request = new UpdateResourceRequest(
                "   ",
                "https://example.com",
                null,
                "DevOps"
        );
        Set<ConstraintViolation<UpdateResourceRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("ResourceResponse.fromEntity should correctly map Resource fields")
    void resourceResponse_FromEntity_CorrectMapping() {
        User user = new User("johndoe", "john@example.com", "hash");
        Resource resource = new Resource(user, "My Resource", "https://example.com", "Description", "General");

        ResourceResponse response = ResourceResponse.fromEntity(resource);

        assertThat(response.getTitle()).isEqualTo("My Resource");
        assertThat(response.getUrl()).isEqualTo("https://example.com");
        assertThat(response.getDescription()).isEqualTo("Description");
        assertThat(response.getCategory()).isEqualTo("General");
    }
}
