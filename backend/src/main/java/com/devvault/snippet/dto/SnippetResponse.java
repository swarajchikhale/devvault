package com.devvault.snippet.dto;

import com.devvault.snippet.Snippet;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing snippet responses returned by the API.
 */
public class SnippetResponse {

    private UUID id;
    private String title;
    private String description;
    private String code;
    private String language;
    private Instant createdAt;
    private Instant updatedAt;

    public SnippetResponse() {
    }

    public SnippetResponse(UUID id, String title, String description, String code, String language, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.code = code;
        this.language = language;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SnippetResponse fromEntity(Snippet snippet) {
        return new SnippetResponse(
                snippet.getId(),
                snippet.getTitle(),
                snippet.getDescription(),
                snippet.getCode(),
                snippet.getLanguage(),
                snippet.getCreatedAt(),
                snippet.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }

    public String getLanguage() {
        return language;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
