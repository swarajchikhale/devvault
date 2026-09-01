package com.devvault.note.dto;

import com.devvault.note.Note;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing note responses returned by the API.
 */
public class NoteResponse {

    private UUID id;
    private String title;
    private String content;
    private String category;
    private Instant createdAt;
    private Instant updatedAt;

    public NoteResponse() {
    }

    public NoteResponse(UUID id, String title, String content, String category, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NoteResponse fromEntity(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getTitle(),
                note.getContent(),
                note.getCategory(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getCategory() {
        return category;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
