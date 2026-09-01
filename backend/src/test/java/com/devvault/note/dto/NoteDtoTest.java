package com.devvault.note.dto;

import com.devvault.note.Note;
import com.devvault.user.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class NoteDtoTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("CreateNoteRequest with valid data should produce no violations")
    void createNoteRequest_ValidData_NoViolations() {
        CreateNoteRequest request = new CreateNoteRequest("Spring Security", "JWT Authentication Guide", "Backend");
        Set<ConstraintViolation<CreateNoteRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("CreateNoteRequest with blank title or content should produce violations")
    void createNoteRequest_BlankFields_HasViolations() {
        CreateNoteRequest request = new CreateNoteRequest("", "", "Backend");
        Set<ConstraintViolation<CreateNoteRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(2);
    }

    @Test
    @DisplayName("CreateNoteRequest with title exceeding 200 chars should produce violation")
    void createNoteRequest_TitleTooLong_HasViolation() {
        String longTitle = "a".repeat(201);
        CreateNoteRequest request = new CreateNoteRequest(longTitle, "Valid Content", "Backend");
        Set<ConstraintViolation<CreateNoteRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("UpdateNoteRequest with valid data should produce no violations")
    void updateNoteRequest_ValidData_NoViolations() {
        UpdateNoteRequest request = new UpdateNoteRequest("Updated Title", "Updated Content", "DevOps");
        Set<ConstraintViolation<UpdateNoteRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("UpdateNoteRequest with blank title should produce violation")
    void updateNoteRequest_BlankTitle_HasViolation() {
        UpdateNoteRequest request = new UpdateNoteRequest("   ", "Valid Content", null);
        Set<ConstraintViolation<UpdateNoteRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(1);
    }

    @Test
    @DisplayName("NoteResponse.fromEntity should correctly map Note fields")
    void noteResponse_FromEntity_CorrectMapping() {
        User user = new User("johndoe", "john@example.com", "hash");
        Note note = new Note(user, "My Note", "My Content", "General");

        NoteResponse response = NoteResponse.fromEntity(note);

        assertThat(response.getTitle()).isEqualTo("My Note");
        assertThat(response.getContent()).isEqualTo("My Content");
        assertThat(response.getCategory()).isEqualTo("General");
    }
}
