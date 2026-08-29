package com.devvault.auth;

import com.devvault.auth.dto.LoginRequest;
import com.devvault.auth.dto.RegisterRequest;
import com.devvault.auth.jwt.JwtService;
import com.devvault.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration test suite for user login flow and JWT token issuance.
 * Uses real Spring application context, real AuthService, JwtService, and PostgreSQL persistence.
 */
@SpringBootTest
class AuthLoginIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

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
    @DisplayName("Should successfully login with username and return valid JWT Bearer token matching user UUID")
    void login_Success_WithUsername() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "loginuser_" + uniqueSuffix;
        String email = "login_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID registeredUserId = UUID.fromString(registerJson.get("id").asText());

        // 2. Login with username
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        // 3. Verify JWT with real JwtService
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(registeredUserId);
    }

    @Test
    @DisplayName("Should successfully login with email and return valid JWT Bearer token matching user UUID")
    void login_Success_WithEmail() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "emailuser_" + uniqueSuffix;
        String email = "email_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID registeredUserId = UUID.fromString(registerJson.get("id").asText());

        // 2. Login with email
        LoginRequest loginRequest = new LoginRequest(email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        // 3. Verify JWT
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(registeredUserId);
    }

    @Test
    @DisplayName("Should return HTTP 401 when logging in with incorrect password")
    void login_IncorrectPassword_ReturnsUnauthorized() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "wrongpass_" + uniqueSuffix;
        String email = "wrongpass_" + uniqueSuffix + "@example.com";
        String password = "CorrectPassword123";

        createdUsernames.add(username);

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 2. Attempt login with wrong password
        LoginRequest loginRequest = new LoginRequest(username, "IncorrectPassword999");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should return HTTP 401 when logging in with unknown identifier")
    void login_UnknownIdentifier_ReturnsUnauthorized() throws Exception {
        String nonExistentIdentifier = "nonexistent_" + UUID.randomUUID().toString().substring(0, 8);

        LoginRequest loginRequest = new LoginRequest(nonExistentIdentifier, "AnyPassword123");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    @DisplayName("Should return HTTP 400 when logging in with blank identifier or password")
    void login_InvalidPayload_ReturnsBadRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest("", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Request validation failed"));
    }

    @Test
    @DisplayName("Should verify JWT security claims and absence of sensitive data")
    void login_JwtSecurityClaimsVerification() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "claimsuser_" + uniqueSuffix;
        String email = "claims_" + uniqueSuffix + "@example.com";
        String password = "SecurePassword123";

        createdUsernames.add(username);

        // 1. Register user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password);
        MvcResult registerResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode registerJson = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID registeredUserId = UUID.fromString(registerJson.get("id").asText());

        // 2. Login
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        // 3. Deep Claims & Security Verification
        assertThat(jwtService.isTokenValid(token)).isTrue();

        Claims claims = jwtService.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo(registeredUserId.toString());
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());

        // Verify claims do not expose sensitive/unnecessary user info
        assertThat(claims.get("password")).isNull();
        assertThat(claims.get("passwordHash")).isNull();
        assertThat(claims.get("username")).isNull();
        assertThat(claims.get("email")).isNull();
    }
}
