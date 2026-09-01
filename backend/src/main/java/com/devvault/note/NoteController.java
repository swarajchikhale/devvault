package com.devvault.note;

import com.devvault.note.dto.CreateNoteRequest;
import com.devvault.note.dto.NoteResponse;
import com.devvault.note.dto.UpdateNoteRequest;
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
 * REST controller for authenticated Note CRUD operations.
 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    private UUID getAuthenticatedUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID)) {
            throw new IllegalStateException("Authentication principal is missing or invalid");
        }
        return (UUID) authentication.getPrincipal();
    }

    /**
     * Creates a new note for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param request the note creation payload
     * @return HTTP 201 Created with the created NoteResponse
     */
    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            Authentication authentication,
            @Valid @RequestBody CreateNoteRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        NoteResponse response = noteService.createNote(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all notes owned by the authenticated user.
     *
     * @param authentication the current security authentication
     * @return HTTP 200 OK with list of NoteResponse objects
     */
    @GetMapping
    public ResponseEntity<List<NoteResponse>> getNotes(Authentication authentication) {
        UUID userId = getAuthenticatedUserId(authentication);
        List<NoteResponse> response = noteService.getNotes(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single note by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the note UUID
     * @return HTTP 200 OK with the NoteResponse
     */
    @GetMapping("/{id}")
    public ResponseEntity<NoteResponse> getNoteById(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        NoteResponse response = noteService.getNoteById(userId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing note by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the note UUID
     * @param request the update note payload
     * @return HTTP 200 OK with the updated NoteResponse
     */
    @PutMapping("/{id}")
    public ResponseEntity<NoteResponse> updateNote(
            Authentication authentication,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateNoteRequest request
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        NoteResponse response = noteService.updateNote(userId, id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a note by ID for the authenticated user.
     *
     * @param authentication the current security authentication
     * @param id the note UUID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNote(
            Authentication authentication,
            @PathVariable UUID id
    ) {
        UUID userId = getAuthenticatedUserId(authentication);
        noteService.deleteNote(userId, id);
        return ResponseEntity.noContent().build();
    }
}
