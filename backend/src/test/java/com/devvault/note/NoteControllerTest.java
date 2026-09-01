package com.devvault.note;

import com.devvault.note.dto.CreateNoteRequest;
import com.devvault.note.dto.NoteResponse;
import com.devvault.note.dto.UpdateNoteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NoteControllerTest {

    @Mock
    private NoteService noteService;

    @InjectMocks
    private NoteController noteController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(noteController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
        authPrincipal = new UsernamePasswordAuthenticationToken(testUserId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/notes should return HTTP 201 Created")
    void createNote_Success() throws Exception {
        CreateNoteRequest request = new CreateNoteRequest("My Title", "My Content", "General");
        UUID noteId = UUID.randomUUID();
        Instant now = Instant.now();
        NoteResponse response = new NoteResponse(noteId, "My Title", "My Content", "General", now, now);

        when(noteService.createNote(eq(testUserId), any(CreateNoteRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/notes")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.title").value("My Title"))
                .andExpect(jsonPath("$.content").value("My Content"))
                .andExpect(jsonPath("$.category").value("General"));

        verify(noteService).createNote(eq(testUserId), any(CreateNoteRequest.class));
    }

    @Test
    @DisplayName("POST /api/notes with invalid payload should return HTTP 400 Bad Request")
    void createNote_InvalidPayload_ReturnsBadRequest() throws Exception {
        CreateNoteRequest invalidRequest = new CreateNoteRequest("", "", null);

        mockMvc.perform(post("/api/notes")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/notes should return HTTP 200 OK with list of notes")
    void getNotes_Success() throws Exception {
        UUID noteId = UUID.randomUUID();
        Instant now = Instant.now();
        NoteResponse response = new NoteResponse(noteId, "Note 1", "Content 1", "Tech", now, now);

        when(noteService.getNotes(testUserId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/notes")
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Note 1"));

        verify(noteService).getNotes(testUserId);
    }

    @Test
    @DisplayName("GET /api/notes/{id} should return HTTP 200 OK with note")
    void getNoteById_Success() throws Exception {
        UUID noteId = UUID.randomUUID();
        Instant now = Instant.now();
        NoteResponse response = new NoteResponse(noteId, "Note 1", "Content 1", "Tech", now, now);

        when(noteService.getNoteById(testUserId, noteId)).thenReturn(response);

        mockMvc.perform(get("/api/notes/" + noteId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.title").value("Note 1"));

        verify(noteService).getNoteById(testUserId, noteId);
    }

    @Test
    @DisplayName("PUT /api/notes/{id} should return HTTP 200 OK with updated note")
    void updateNote_Success() throws Exception {
        UUID noteId = UUID.randomUUID();
        UpdateNoteRequest request = new UpdateNoteRequest("Updated Title", "Updated Content", "New Cat");
        Instant now = Instant.now();
        NoteResponse response = new NoteResponse(noteId, "Updated Title", "Updated Content", "New Cat", now, now);

        when(noteService.updateNote(eq(testUserId), eq(noteId), any(UpdateNoteRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/notes/" + noteId)
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"));

        verify(noteService).updateNote(eq(testUserId), eq(noteId), any(UpdateNoteRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/notes/{id} should return HTTP 204 No Content")
    void deleteNote_Success() throws Exception {
        UUID noteId = UUID.randomUUID();

        mockMvc.perform(delete("/api/notes/" + noteId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isNoContent());

        verify(noteService).deleteNote(testUserId, noteId);
    }
}
