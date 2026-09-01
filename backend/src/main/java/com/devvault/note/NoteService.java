package com.devvault.note;

import com.devvault.note.dto.CreateNoteRequest;
import com.devvault.note.dto.NoteResponse;
import com.devvault.note.dto.UpdateNoteRequest;
import com.devvault.note.exception.NoteNotFoundException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing note creation, retrieval, updates, and deletion with strict user ownership enforcement.
 */
@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;

    public NoteService(NoteRepository noteRepository, UserRepository userRepository) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new note for the specified authenticated user.
     *
     * @param userId the owner user's UUID
     * @param request the note creation payload
     * @return the created NoteResponse
     */
    @Transactional
    public NoteResponse createNote(UUID userId, CreateNoteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + userId));

        Note note = new Note(user, request.getTitle(), request.getContent(), request.getCategory());
        Note savedNote = noteRepository.save(note);

        return NoteResponse.fromEntity(savedNote);
    }

    /**
     * Retrieves all notes belonging to the authenticated user.
     *
     * @param userId the owner user's UUID
     * @return list of NoteResponse objects
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> getNotes(UUID userId) {
        return noteRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(NoteResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single note by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param noteId the note UUID
     * @return the NoteResponse if found and owned by the user
     * @throws NoteNotFoundException if the note is not found or not owned by user
     */
    @Transactional(readOnly = true)
    public NoteResponse getNoteById(UUID userId, UUID noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with ID: " + noteId));

        return NoteResponse.fromEntity(note);
    }

    /**
     * Updates an existing note by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param noteId the note UUID
     * @param request the updated note payload
     * @return the updated NoteResponse
     * @throws NoteNotFoundException if the note is not found or not owned by user
     */
    @Transactional
    public NoteResponse updateNote(UUID userId, UUID noteId, UpdateNoteRequest request) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with ID: " + noteId));

        note.setTitle(request.getTitle());
        note.setContent(request.getContent());
        note.setCategory(request.getCategory());

        Note savedNote = noteRepository.save(note);
        return NoteResponse.fromEntity(savedNote);
    }

    /**
     * Deletes an existing note by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param noteId the note UUID
     * @throws NoteNotFoundException if the note is not found or not owned by user
     */
    @Transactional
    public void deleteNote(UUID userId, UUID noteId) {
        Note note = noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new NoteNotFoundException("Note not found with ID: " + noteId));

        noteRepository.delete(note);
    }
}
