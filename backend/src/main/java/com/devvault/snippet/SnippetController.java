package com.devvault.snippet;

import com.devvault.snippet.dto.CreateSnippetRequest;
import com.devvault.snippet.dto.SnippetResponse;
import com.devvault.snippet.dto.UpdateSnippetRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for authenticated Snippet CRUD operations.
 */
@RestController
@RequestMapping("/api/snippets")
public class SnippetController {

    private final SnippetService snippetService;

    public SnippetController(SnippetService snippetService) {
        this.snippetService = snippetService;
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new IllegalStateException("Authentication principal is missing or invalid");
        }
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Creates a new snippet for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param request the snippet creation payload
     * @return HTTP 201 Created with the created SnippetResponse
     */
    @PostMapping
    public ResponseEntity<SnippetResponse> createSnippet(
            Authentication authentication,
            @Valid @RequestBody CreateSnippetRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        SnippetResponse response = snippetService.createSnippet(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all snippets owned by the authenticated user.
     *
     * @param authentication the current security authentication
     * @return HTTP 200 OK with list of SnippetResponse objects
     */
    @GetMapping
    public ResponseEntity<List<SnippetResponse>> getSnippets(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        List<SnippetResponse> response = snippetService.getSnippets(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single snippet by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the snippet UUID
     * @return HTTP 200 OK with the SnippetResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<SnippetResponse> getSnippetById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        SnippetResponse response = snippetService.getSnippetById(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing snippet by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the snippet UUID
     * @param request the update snippet payload
     * @return HTTP 200 OK with the updated SnippetResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<SnippetResponse> updateSnippet(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSnippetRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        SnippetResponse response = snippetService.updateSnippet(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a snippet by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the snippet UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSnippet(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        snippetService.deleteSnippet(userId, id);
        return ResponseEntity.noContent().build();
    }
}
