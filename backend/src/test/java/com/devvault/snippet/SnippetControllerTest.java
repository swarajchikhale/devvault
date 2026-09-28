package com.devvault.snippet;

import com.devvault.snippet.dto.CreateSnippetRequest;
import com.devvault.snippet.dto.SnippetResponse;
import com.devvault.snippet.dto.UpdateSnippetRequest;
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
class SnippetControllerTest {

    @Mock
    private SnippetService snippetService;

    @InjectMocks
    private SnippetController snippetController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(snippetController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
        authPrincipal = new UsernamePasswordAuthenticationToken(testUserId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/snippets should return HTTP 201 Created")
    void createSnippet_Success() throws Exception {
        CreateSnippetRequest request = new CreateSnippetRequest("Read File", "Helper", "Files.read();", "java");
        UUID snippetId = UUID.randomUUID();
        Instant now = Instant.now();
        SnippetResponse response = new SnippetResponse(snippetId, "Read File", "Helper", "Files.read();", "java", now, now);

        when(snippetService.createSnippet(eq(testUserId), any(CreateSnippetRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/snippets")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(snippetId.toString()))
                .andExpect(jsonPath("$.title").value("Read File"))
                .andExpect(jsonPath("$.description").value("Helper"))
                .andExpect(jsonPath("$.code").value("Files.read();"))
                .andExpect(jsonPath("$.language").value("java"));

        verify(snippetService).createSnippet(eq(testUserId), any(CreateSnippetRequest.class));
    }

    @Test
    @DisplayName("POST /api/snippets with invalid payload should return HTTP 400 Bad Request")
    void createSnippet_InvalidPayload_ReturnsBadRequest() throws Exception {
        CreateSnippetRequest invalidRequest = new CreateSnippetRequest("", null, "", "");

        mockMvc.perform(post("/api/snippets")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/snippets should return HTTP 200 OK with list of snippets")
    void getSnippets_Success() throws Exception {
        UUID snippetId = UUID.randomUUID();
        Instant now = Instant.now();
        SnippetResponse response = new SnippetResponse(snippetId, "Snippet 1", "Desc", "code", "python", now, now);

        when(snippetService.getSnippets(testUserId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/snippets")
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Snippet 1"));

        verify(snippetService).getSnippets(testUserId);
    }

    @Test
    @DisplayName("GET /api/snippets/{id} should return HTTP 200 OK with snippet")
    void getSnippetById_Success() throws Exception {
        UUID snippetId = UUID.randomUUID();
        Instant now = Instant.now();
        SnippetResponse response = new SnippetResponse(snippetId, "Snippet 1", "Desc", "code", "python", now, now);

        when(snippetService.getSnippetById(testUserId, snippetId)).thenReturn(response);

        mockMvc.perform(get("/api/snippets/" + snippetId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(snippetId.toString()))
                .andExpect(jsonPath("$.title").value("Snippet 1"));

        verify(snippetService).getSnippetById(testUserId, snippetId);
    }

    @Test
    @DisplayName("PUT /api/snippets/{id} should return HTTP 200 OK with updated snippet")
    void updateSnippet_Success() throws Exception {
        UUID snippetId = UUID.randomUUID();
        UpdateSnippetRequest request = new UpdateSnippetRequest("Updated Title", "Updated Desc", "new code", "kotlin");
        Instant now = Instant.now();
        SnippetResponse response = new SnippetResponse(snippetId, "Updated Title", "Updated Desc", "new code", "kotlin", now, now);

        when(snippetService.updateSnippet(eq(testUserId), eq(snippetId), any(UpdateSnippetRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/snippets/" + snippetId)
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description").value("Updated Desc"))
                .andExpect(jsonPath("$.code").value("new code"))
                .andExpect(jsonPath("$.language").value("kotlin"));

        verify(snippetService).updateSnippet(eq(testUserId), eq(snippetId), any(UpdateSnippetRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/snippets/{id} should return HTTP 204 No Content")
    void deleteSnippet_Success() throws Exception {
        UUID snippetId = UUID.randomUUID();

        mockMvc.perform(delete("/api/snippets/" + snippetId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isNoContent());

        verify(snippetService).deleteSnippet(testUserId, snippetId);
    }
}
