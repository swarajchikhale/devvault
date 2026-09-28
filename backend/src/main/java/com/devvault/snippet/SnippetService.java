package com.devvault.snippet;

import com.devvault.snippet.dto.CreateSnippetRequest;
import com.devvault.snippet.dto.SnippetResponse;
import com.devvault.snippet.dto.UpdateSnippetRequest;
import com.devvault.snippet.exception.SnippetNotFoundException;
import com.devvault.user.User;
import com.devvault.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing snippet creation, retrieval, updates, and deletion with strict user ownership enforcement.
 */
@Service
public class SnippetService {

    private final SnippetRepository snippetRepository;
    private final UserRepository userRepository;

    public SnippetService(SnippetRepository snippetRepository, UserRepository userRepository) {
        this.snippetRepository = snippetRepository;
        this.userRepository = userRepository;
    }

    /**
     * Creates a new snippet for the specified authenticated user.
     *
     * @param userId the owner user's UUID
     * @param request the snippet creation payload
     * @return the created SnippetResponse
     */
    @Transactional
    public SnippetResponse createSnippet(UUID userId, CreateSnippetRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found for ID: " + userId));

        Snippet snippet = new Snippet(
                user,
                request.getTitle(),
                request.getDescription(),
                request.getCode(),
                request.getLanguage()
        );
        Snippet savedSnippet = snippetRepository.save(snippet);

        return SnippetResponse.fromEntity(savedSnippet);
    }

    /**
     * Retrieves all snippets belonging to the authenticated user.
     *
     * @param userId the owner user's UUID
     * @return list of SnippetResponse objects
     */
    @Transactional(readOnly = true)
    public List<SnippetResponse> getSnippets(UUID userId) {
        return snippetRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(SnippetResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single snippet by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param snippetId the snippet UUID
     * @return the SnippetResponse if found and owned by the user
     * @throws SnippetNotFoundException if the snippet is not found or not owned by user
     */
    @Transactional(readOnly = true)
    public SnippetResponse getSnippetById(UUID userId, UUID snippetId) {
        Snippet snippet = snippetRepository.findByIdAndUserId(snippetId, userId)
                .orElseThrow(() -> new SnippetNotFoundException("Snippet not found with ID: " + snippetId));

        return SnippetResponse.fromEntity(snippet);
    }

    /**
     * Updates an existing snippet by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param snippetId the snippet UUID
     * @param request the updated snippet payload
     * @return the updated SnippetResponse
     * @throws SnippetNotFoundException if the snippet is not found or not owned by user
     */
    @Transactional
    public SnippetResponse updateSnippet(UUID userId, UUID snippetId, UpdateSnippetRequest request) {
        Snippet snippet = snippetRepository.findByIdAndUserId(snippetId, userId)
                .orElseThrow(() -> new SnippetNotFoundException("Snippet not found with ID: " + snippetId));

        snippet.setTitle(request.getTitle());
        snippet.setDescription(request.getDescription());
        snippet.setCode(request.getCode());
        snippet.setLanguage(request.getLanguage());

        Snippet savedSnippet = snippetRepository.save(snippet);
        return SnippetResponse.fromEntity(savedSnippet);
    }

    /**
     * Deletes an existing snippet by ID, strictly verifying user ownership.
     *
     * @param userId the owner user's UUID
     * @param snippetId the snippet UUID
     * @throws SnippetNotFoundException if the snippet is not found or not owned by user
     */
    @Transactional
    public void deleteSnippet(UUID userId, UUID snippetId) {
        Snippet snippet = snippetRepository.findByIdAndUserId(snippetId, userId)
                .orElseThrow(() -> new SnippetNotFoundException("Snippet not found with ID: " + snippetId));

        snippetRepository.delete(snippet);
    }
}
