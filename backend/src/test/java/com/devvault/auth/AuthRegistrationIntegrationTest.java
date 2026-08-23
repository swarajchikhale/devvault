package com.devvault.auth;

import com.devvault.auth.dto.RegisterRequest;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test suite for user registration API.
 * Uses real Spring application context, real AuthService, and database persistence.
 */
@SpringBootTest
class AuthRegistrationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> createdUsernames = new ArrayList<>();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        createdUsernames.clear();
    }

    @AfterEach
    void tearDown() {
        for (String username : createdUsernames) {
            userRepository.findByUsername(username).ifPresent(userRepository::delete);
        }
        createdUsernames.clear();
    }

    @Test
    @DisplayName("Should successfully register user, persist in database, and hash password")
    void register_Success_PersistsInDatabase() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "integrationuser_" + uniqueSuffix;
        String email = "integration_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        RegisterRequest request = new RegisterRequest(username, email, password);

        MvcResult result = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID returnedId = UUID.fromString(responseJson.get("id").asText());

        Optional<User> persistedUserOptional = userRepository.findById(returnedId);
        assertThat(persistedUserOptional).isPresent();

        User persistedUser = persistedUserOptional.get();
        assertThat(persistedUser.getUsername()).isEqualTo(username);
        assertThat(persistedUser.getEmail()).isEqualTo(email);
        assertThat(persistedUser.getPasswordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, persistedUser.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should return HTTP 409 when registering with duplicate username")
    void register_DuplicateUsername_ReturnsConflict() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "dupuser_" + uniqueSuffix;
        String email1 = "user1_" + uniqueSuffix + "@example.com";
        String email2 = "user2_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        RegisterRequest firstRequest = new RegisterRequest(username, email1, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        RegisterRequest secondRequest = new RegisterRequest(username, email2, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Username is already taken"));
    }

    @Test
    @DisplayName("Should return HTTP 409 when registering with duplicate email")
    void register_DuplicateEmail_ReturnsConflict() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username1 = "user1_" + uniqueSuffix;
        String username2 = "user2_" + uniqueSuffix;
        String email = "dupemail_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username1);
        createdUsernames.add(username2);

        RegisterRequest firstRequest = new RegisterRequest(username1, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        RegisterRequest secondRequest = new RegisterRequest(username2, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @DisplayName("Should return HTTP 400 when registering with invalid payload")
    void register_InvalidPayload_ReturnsBadRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("", "invalid-email-format", "");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }
}
