package com.devvault.note;

import com.devvault.auth.dto.LoginRequest;
import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.jwt.JwtService;
import com.devvault.note.dto.CreateNoteRequest;
import com.devvault.note.dto.UpdateNoteRequest;
import com.devvault.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class NoteIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<UUID> createdNoteIds = new ArrayList<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        createdUsernames.clear();
        createdNoteIds.clear();
    }

    @AfterEach
    void tearDown() {
        for (UUID noteId : createdNoteIds) {
            noteRepository.findById(noteId).ifPresent(noteRepository::delete);
        }
        createdNoteIds.clear();

        for (String username : createdUsernames) {
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
        }
        createdUsernames.clear();
    }

    private String registerAndLoginUser(String prefix) throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = prefix + "_" + uniqueSuffix;
        String email = prefix + "_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return json.get("accessToken").asText();
    }

    @Test
    @DisplayName("Should perform full CRUD cycle on notes for authenticated user")
    void note_Crud_Success() throws Exception {
        String token = registerAndLoginUser("cruduser");

        // 1. Create Note
        CreateNoteRequest createRequest = new CreateNoteRequest("Database Indexing", "B-Tree vs Hash Indexing", "Database");
        MvcResult createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Database Indexing"))
                .andExpect(jsonPath("$.content").value("B-Tree vs Hash Indexing"))
                .andExpect(jsonPath("$.category").value("Database"))
                .andReturn();

        UUID noteId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdNoteIds.add(noteId);

        // 2. Get Notes List
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(noteId.toString()));

        // 3. Get Single Note by ID
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noteId.toString()))
                .andExpect(jsonPath("$.title").value("Database Indexing"));

        // 4. Update Note
        UpdateNoteRequest updateRequest = new UpdateNoteRequest("Database Indexing v2", "Updated content", "DBA");
        mockMvc.perform(put("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Database Indexing v2"))
                .andExpect(jsonPath("$.content").value("Updated content"))
                .andExpect(jsonPath("$.category").value("DBA"));

        // 5. Delete Note
        mockMvc.perform(delete("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify Deletion
        mockMvc.perform(get("/api/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should enforce strict user ownership: User B cannot access User A's note")
    void note_UserOwnershipIsolation_Enforced() throws Exception {
        String tokenUserA = registerAndLoginUser("usera");
        String tokenUserB = registerAndLoginUser("userb");

        // User A creates a note
        CreateNoteRequest createRequest = new CreateNoteRequest("Private Note A", "Secret content", "Private");
        MvcResult createResult = mockMvc.perform(post("/api/notes")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID noteIdA = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdNoteIds.add(noteIdA);

        // User B attempts to read User A's note -> 404 Not Found
        mockMvc.perform(get("/api/notes/" + noteIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"));

        // User B attempts to update User A's note -> 404 Not Found
        UpdateNoteRequest updateRequest = new UpdateNoteRequest("Hijacked", "Hijacked content", "Hacked");
        mockMvc.perform(put("/api/notes/" + noteIdA)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"));

        // User B attempts to delete User A's note -> 404 Not Found
        mockMvc.perform(delete("/api/notes/" + noteIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOTE_NOT_FOUND"));

        // User B's note list should be empty
        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests with HTTP 401 Unauthorized")
    void note_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/notes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should reject requests with valid JWT for non-existent user with HTTP 401 Unauthorized")
    void note_ValidJwtWithNonExistentUser_ReturnsUnauthorized() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String token = jwtService.generateToken(nonExistentUserId);

        mockMvc.perform(get("/api/notes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
