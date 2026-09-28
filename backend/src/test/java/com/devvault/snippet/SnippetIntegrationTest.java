package com.devvault.snippet;

import com.devvault.auth.dto.LoginRequest;
import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.jwt.JwtService;
import com.devvault.snippet.dto.CreateSnippetRequest;
import com.devvault.snippet.dto.UpdateSnippetRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class SnippetIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SnippetRepository snippetRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<UUID> createdSnippetIds = new ArrayList<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        createdUsernames.clear();
        createdSnippetIds.clear();
    }

    @AfterEach
    void tearDown() {
        for (UUID snippetId : createdSnippetIds) {
            snippetRepository.findById(snippetId).ifPresent(snippetRepository::delete);
        }
        createdSnippetIds.clear();

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
    @DisplayName("Should perform full CRUD cycle on snippets for authenticated user")
    void snippet_Crud_Success() throws Exception {
        String token = registerAndLoginUser("cruduser");

        // 1. Create Snippet
        CreateSnippetRequest createRequest = new CreateSnippetRequest(
                "Read File in Java",
                "Helper to read file contents",
                "Files.readString(Path.of(\"file.txt\"));",
                "java"
        );
        MvcResult createResult = mockMvc.perform(post("/api/snippets")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Read File in Java"))
                .andExpect(jsonPath("$.description").value("Helper to read file contents"))
                .andExpect(jsonPath("$.code").value("Files.readString(Path.of(\"file.txt\"));"))
                .andExpect(jsonPath("$.language").value("java"))
                .andReturn();

        UUID snippetId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdSnippetIds.add(snippetId);

        // 2. Get Snippets List
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(snippetId.toString()));

        // 3. Get Single Snippet by ID
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(snippetId.toString()))
                .andExpect(jsonPath("$.title").value("Read File in Java"));

        // 4. Update Snippet
        UpdateSnippetRequest updateRequest = new UpdateSnippetRequest(
                "Read File in Java NIO",
                "Updated file helper",
                "Files.readString(Path.of(\"example.txt\"), StandardCharsets.UTF_8);",
                "java"
        );
        mockMvc.perform(put("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Read File in Java NIO"))
                .andExpect(jsonPath("$.description").value("Updated file helper"))
                .andExpect(jsonPath("$.code").value("Files.readString(Path.of(\"example.txt\"), StandardCharsets.UTF_8);"))
                .andExpect(jsonPath("$.language").value("java"));

        // 5. Delete Snippet
        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify Deletion -> 404 Not Found
        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SNIPPET_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should enforce strict user ownership: User B cannot access User A's snippet")
    void snippet_UserOwnershipIsolation_Enforced() throws Exception {
        String tokenUserA = registerAndLoginUser("usera");
        String tokenUserB = registerAndLoginUser("userb");

        // User A creates a snippet
        CreateSnippetRequest createRequest = new CreateSnippetRequest(
                "Private Algorithm",
                "Secret logic",
                "secret_func();",
                "python"
        );
        MvcResult createResult = mockMvc.perform(post("/api/snippets")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID snippetIdA = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdSnippetIds.add(snippetIdA);

        // User B attempts to read User A's snippet -> 404 Not Found
        mockMvc.perform(get("/api/snippets/" + snippetIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SNIPPET_NOT_FOUND"));

        // User B attempts to update User A's snippet -> 404 Not Found
        UpdateSnippetRequest updateRequest = new UpdateSnippetRequest("Hijacked", "Hijacked", "malicious()", "python");
        mockMvc.perform(put("/api/snippets/" + snippetIdA)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SNIPPET_NOT_FOUND"));

        // User B attempts to delete User A's snippet -> 404 Not Found
        mockMvc.perform(delete("/api/snippets/" + snippetIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("SNIPPET_NOT_FOUND"));

        // User B's snippet list should be empty
        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests with HTTP 401 Unauthorized")
    void snippet_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/snippets"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should reject requests with valid JWT for non-existent user with HTTP 401 Unauthorized")
    void snippet_ValidJwtWithNonExistentUser_ReturnsUnauthorized() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String token = jwtService.generateToken(nonExistentUserId);

        mockMvc.perform(get("/api/snippets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
