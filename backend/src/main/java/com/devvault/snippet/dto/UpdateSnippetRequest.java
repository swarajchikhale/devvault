package com.devvault.snippet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating an existing code snippet.
 */
public class UpdateSnippetRequest {

    @NotBlank(message = "Title must not be blank")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotBlank(message = "Code must not be blank")
    private String code;

    @NotBlank(message = "Language must not be blank")
    @Size(max = 50, message = "Language must not exceed 50 characters")
    private String language;

    public UpdateSnippetRequest() {
    }

    public UpdateSnippetRequest(String title, String description, String code, String language) {
        this.title = title;
        this.description = description;
        this.code = code;
        this.language = language;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
