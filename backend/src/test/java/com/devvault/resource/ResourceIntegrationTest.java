package com.devvault.resource;

import com.devvault.auth.dto.LoginRequest;
import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.jwt.JwtService;
import com.devvault.resource.dto.CreateResourceRequest;
import com.devvault.resource.dto.UpdateResourceRequest;
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
class ResourceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private Filter springSecurityFilterChain;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private JwtService jwtService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> createdUsernames = new ArrayList<>();
    private final List<UUID> createdResourceIds = new ArrayList<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
        createdUsernames.clear();
        createdResourceIds.clear();
    }

    @AfterEach
    void tearDown() {
        for (UUID resourceId : createdResourceIds) {
            resourceRepository.findById(resourceId).ifPresent(resourceRepository::delete);
        }
        createdResourceIds.clear();

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
    @DisplayName("Should perform full CRUD cycle on resources for authenticated user")
    void resource_Crud_Success() throws Exception {
        String token = registerAndLoginUser("crudresuser");

        // 1. Create Resource
        CreateResourceRequest createRequest = new CreateResourceRequest(
                "Spring Framework Docs",
                "https://spring.io/projects/spring-framework",
                "Official Spring Framework documentation",
                "Documentation"
        );
        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.title").value("Spring Framework Docs"))
                .andExpect(jsonPath("$.url").value("https://spring.io/projects/spring-framework"))
                .andExpect(jsonPath("$.description").value("Official Spring Framework documentation"))
                .andExpect(jsonPath("$.category").value("Documentation"))
                .andReturn();

        UUID resourceId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdResourceIds.add(resourceId);

        // 2. Get Resources List
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(resourceId.toString()));

        // 3. Get Single Resource by ID
        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(resourceId.toString()))
                .andExpect(jsonPath("$.title").value("Spring Framework Docs"));

        // 4. Update Resource
        UpdateResourceRequest updateRequest = new UpdateResourceRequest(
                "Spring Boot Docs Overview",
                "https://spring.io/projects/spring-boot",
                "Updated Spring Boot docs reference",
                "Guides"
        );
        mockMvc.perform(put("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Spring Boot Docs Overview"))
                .andExpect(jsonPath("$.url").value("https://spring.io/projects/spring-boot"))
                .andExpect(jsonPath("$.description").value("Updated Spring Boot docs reference"))
                .andExpect(jsonPath("$.category").value("Guides"));

        // 5. Delete Resource
        mockMvc.perform(delete("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        // Verify Deletion -> 404 Not Found
        mockMvc.perform(get("/api/resources/" + resourceId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should enforce strict user ownership: User B cannot access User A's resource")
    void resource_UserOwnershipIsolation_Enforced() throws Exception {
        String tokenUserA = registerAndLoginUser("resusera");
        String tokenUserB = registerAndLoginUser("resuserb");

        // User A creates a resource
        CreateResourceRequest createRequest = new CreateResourceRequest(
                "Private Vault Repo",
                "https://github.com/private/vault",
                "Internal link",
                "Repository"
        );
        MvcResult createResult = mockMvc.perform(post("/api/resources")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID resourceIdA = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText());
        createdResourceIds.add(resourceIdA);

        // User B attempts to read User A's resource -> 404 Not Found
        mockMvc.perform(get("/api/resources/" + resourceIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        // User B attempts to update User A's resource -> 404 Not Found
        UpdateResourceRequest updateRequest = new UpdateResourceRequest("Hijacked", "https://malicious.com", "Hijacked", "Hacking");
        mockMvc.perform(put("/api/resources/" + resourceIdA)
                        .header("Authorization", "Bearer " + tokenUserB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        // User B attempts to delete User A's resource -> 404 Not Found
        mockMvc.perform(delete("/api/resources/" + resourceIdA)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        // User B's resource list should be empty
        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Should reject unauthenticated requests with HTTP 401 Unauthorized")
    void resource_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("Should reject requests with valid JWT for non-existent user with HTTP 401 Unauthorized")
    void resource_ValidJwtWithNonExistentUser_ReturnsUnauthorized() throws Exception {
        UUID nonExistentUserId = UUID.randomUUID();
        String token = jwtService.generateToken(nonExistentUserId);

        mockMvc.perform(get("/api/resources")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }
}
