package com.devvault.resource.dto;

import com.devvault.resource.Resource;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO representing resource bookmark responses returned by the API.
 */
public class ResourceResponse {

    private UUID id;
    private String title;
    private String url;
    private String description;
    private String category;
    private Instant createdAt;
    private Instant updatedAt;

    public ResourceResponse() {
    }

    public ResourceResponse(UUID id, String title, String url, String description, String category, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.description = description;
        this.category = category;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ResourceResponse fromEntity(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getTitle(),
                resource.getUrl(),
                resource.getDescription(),
                resource.getCategory(),
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getDescription() {
        return description;
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
