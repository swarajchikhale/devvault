package com.devvault.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * DTO for creating a new resource bookmark.
 */
public class CreateResourceRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "URL must not be blank")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @URL(regexp = "^https?://.*", message = "URL must be valid")
    private String url;

    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    public CreateResourceRequest() {
    }

    public CreateResourceRequest(String title, String url, String description, String category) {
        this.title = title;
        this.url = url;
        this.description = description;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
