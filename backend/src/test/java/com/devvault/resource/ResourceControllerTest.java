package com.devvault.resource;

import com.devvault.resource.dto.CreateResourceRequest;
import com.devvault.resource.dto.ResourceResponse;
import com.devvault.resource.dto.UpdateResourceRequest;
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
class ResourceControllerTest {

    @Mock
    private ResourceService resourceService;

    @InjectMocks
    private ResourceController resourceController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UUID testUserId;
    private UsernamePasswordAuthenticationToken authPrincipal;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(resourceController)
                .setValidator(validator)
                .build();
        objectMapper = new ObjectMapper();
        testUserId = UUID.randomUUID();
        authPrincipal = new UsernamePasswordAuthenticationToken(testUserId, null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(authPrincipal);
    }

    @Test
    @DisplayName("POST /api/resources should return HTTP 201 Created")
    void createResource_Success() throws Exception {
        CreateResourceRequest request = new CreateResourceRequest("Spring Boot Docs", "https://spring.io/projects/spring-boot", "Official docs", "Documentation");
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceResponse response = new ResourceResponse(resourceId, "Spring Boot Docs", "https://spring.io/projects/spring-boot", "Official docs", "Documentation", now, now);

        when(resourceService.createResource(eq(testUserId), any(CreateResourceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/resources")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.title").value("Spring Boot Docs"))
                .andExpect(jsonPath("$.url").value("https://spring.io/projects/spring-boot"))
                .andExpect(jsonPath("$.description").value("Official docs"))
                .andExpect(jsonPath("$.category").value("Documentation"));

        verify(resourceService).createResource(eq(testUserId), any(CreateResourceRequest.class));
    }

    @Test
    @DisplayName("POST /api/resources with invalid payload should return HTTP 400 Bad Request")
    void createResource_InvalidPayload_ReturnsBadRequest() throws Exception {
        CreateResourceRequest invalidRequest = new CreateResourceRequest("", "invalid-url", null, null);

        mockMvc.perform(post("/api/resources")
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/resources should return HTTP 200 OK with list of resources")
    void getResources_Success() throws Exception {
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceResponse response = new ResourceResponse(resourceId, "Resource 1", "https://example.com/1", "Desc", "Cat", now, now);

        when(resourceService.getResources(testUserId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/resources")
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Resource 1"));

        verify(resourceService).getResources(testUserId);
    }

    @Test
    @DisplayName("GET /api/resources/{id} should return HTTP 200 OK with resource")
    void getResourceById_Success() throws Exception {
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceResponse response = new ResourceResponse(resourceId, "Resource 1", "https://example.com/1", "Desc", "Cat", now, now);

        when(resourceService.getResourceById(testUserId, resourceId)).thenReturn(response);

        mockMvc.perform(get("/api/resources/" + resourceId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.title").value("Resource 1"));

        verify(resourceService).getResourceById(testUserId, resourceId);
    }

    @Test
    @DisplayName("PUT /api/resources/{id} should return HTTP 200 OK with updated resource")
    void updateResource_Success() throws Exception {
        UUID resourceId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("Updated Title", "https://updated.com", "Updated Desc", "Updated Cat");
        Instant now = Instant.now();
        ResourceResponse response = new ResourceResponse(resourceId, "Updated Title", "https://updated.com", "Updated Desc", "Updated Cat", now, now);

        when(resourceService.updateResource(eq(testUserId), eq(resourceId), any(UpdateResourceRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/resources/" + resourceId)
                        .principal((Principal) authPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.url").value("https://updated.com"))
                .andExpect(jsonPath("$.description").value("Updated Desc"))
                .andExpect(jsonPath("$.category").value("Updated Cat"));

        verify(resourceService).updateResource(eq(testUserId), eq(resourceId), any(UpdateResourceRequest.class));
    }

    @Test
    @DisplayName("DELETE /api/resources/{id} should return HTTP 204 No Content")
    void deleteResource_Success() throws Exception {
        UUID resourceId = UUID.randomUUID();

        mockMvc.perform(delete("/api/resources/" + resourceId)
                        .principal((Principal) authPrincipal))
                .andExpect(status().isNoContent());

        verify(resourceService).deleteResource(testUserId, resourceId);
    }
}
