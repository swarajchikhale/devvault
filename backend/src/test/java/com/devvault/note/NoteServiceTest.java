package com.devvault.note;

import com.devvault.note.dto.CreateNoteRequest;
import com.devvault.note.dto.NoteResponse;
import com.devvault.note.dto.UpdateNoteRequest;
import com.devvault.note.exception.NoteNotFoundException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    @Mock
    private NoteRepository noteRepository;

    @Mock
    private UserRepository userRepository;

    private NoteService noteService;

    @BeforeEach
    void setUp() {
        noteService = new NoteService(noteRepository, userRepository);
    }

    @Test
    @DisplayName("Should create note successfully for authenticated user")
    void createNote_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        CreateNoteRequest request = new CreateNoteRequest("My Title", "My Content", "Backend");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NoteResponse response = noteService.createNote(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("My Title");
        assertThat(response.getContent()).isEqualTo("My Content");
        assertThat(response.getCategory()).isEqualTo("Backend");

        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should retrieve all notes for authenticated user")
    void getNotes_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Note note1 = new Note(user, "Title 1", "Content 1", "Cat 1");
        Note note2 = new Note(user, "Title 2", "Content 2", "Cat 2");

        when(noteRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(note1, note2));

        List<NoteResponse> responses = noteService.getNotes(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(responses.get(1).getTitle()).isEqualTo("Title 2");
    }

    @Test
    @DisplayName("Should retrieve note by ID when owned by user")
    void getNoteById_Success() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Note note = new Note(user, "Title", "Content", "Cat");

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(note));

        NoteResponse response = noteService.getNoteById(userId, noteId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Title");
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException when retrieving note not owned by user")
    void getNoteById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.getNoteById(userId, noteId))
                .isInstanceOf(NoteNotFoundException.class)
                .hasMessageContaining("Note not found with ID: " + noteId);
    }

    @Test
    @DisplayName("Should update note when owned by user")
    void updateNote_Success() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Note existingNote = new Note(user, "Old Title", "Old Content", "Old Cat");
        UpdateNoteRequest request = new UpdateNoteRequest("New Title", "New Content", "New Cat");

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(existingNote));
        when(noteRepository.save(existingNote)).thenReturn(existingNote);

        NoteResponse response = noteService.updateNote(userId, noteId, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getContent()).isEqualTo("New Content");
        assertThat(response.getCategory()).isEqualTo("New Cat");
        verify(noteRepository).save(existingNote);
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException when updating note not owned by user")
    void updateNote_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        UpdateNoteRequest request = new UpdateNoteRequest("New Title", "New Content", "New Cat");

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.updateNote(userId, noteId, request))
                .isInstanceOf(NoteNotFoundException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete note when owned by user")
    void deleteNote_Success() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Note existingNote = new Note(user, "Title", "Content", "Cat");

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.of(existingNote));

        noteService.deleteNote(userId, noteId);

        verify(noteRepository).delete(existingNote);
    }

    @Test
    @DisplayName("Should throw NoteNotFoundException when deleting note not owned by user")
    void deleteNote_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID noteId = UUID.randomUUID();

        when(noteRepository.findByIdAndUserId(noteId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.deleteNote(userId, noteId))
                .isInstanceOf(NoteNotFoundException.class);

        verify(noteRepository, never()).delete(any());
    }
}
