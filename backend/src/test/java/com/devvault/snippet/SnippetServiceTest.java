package com.devvault.snippet;

import com.devvault.snippet.dto.CreateSnippetRequest;
import com.devvault.snippet.dto.SnippetResponse;
import com.devvault.snippet.dto.UpdateSnippetRequest;
import com.devvault.snippet.exception.SnippetNotFoundException;
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
class SnippetServiceTest {

    @Mock
    private SnippetRepository snippetRepository;

    @Mock
    private UserRepository userRepository;

    private SnippetService snippetService;

    @BeforeEach
    void setUp() {
        snippetService = new SnippetService(snippetRepository, userRepository);
    }

    @Test
    @DisplayName("Should create snippet successfully for authenticated user")
    void createSnippet_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        CreateSnippetRequest request = new CreateSnippetRequest("Read File", "File helper", "Files.readString();", "java");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(snippetRepository.save(any(Snippet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SnippetResponse response = snippetService.createSnippet(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Read File");
        assertThat(response.getDescription()).isEqualTo("File helper");
        assertThat(response.getCode()).isEqualTo("Files.readString();");
        assertThat(response.getLanguage()).isEqualTo("java");

        ArgumentCaptor<Snippet> snippetCaptor = ArgumentCaptor.forClass(Snippet.class);
        verify(snippetRepository).save(snippetCaptor.capture());
        assertThat(snippetCaptor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should retrieve all snippets for authenticated user")
    void getSnippets_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Snippet snippet1 = new Snippet(user, "Title 1", "Desc 1", "code 1", "java");
        Snippet snippet2 = new Snippet(user, "Title 2", "Desc 2", "code 2", "python");

        when(snippetRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(snippet1, snippet2));

        List<SnippetResponse> responses = snippetService.getSnippets(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(responses.get(1).getTitle()).isEqualTo("Title 2");
    }

    @Test
    @DisplayName("Should retrieve snippet by ID when owned by user")
    void getSnippetById_Success() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Snippet snippet = new Snippet(user, "Title", "Desc", "code", "java");

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.of(snippet));

        SnippetResponse response = snippetService.getSnippetById(userId, snippetId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Title");
    }

    @Test
    @DisplayName("Should throw SnippetNotFoundException when retrieving snippet not owned by user")
    void getSnippetById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snippetService.getSnippetById(userId, snippetId))
                .isInstanceOf(SnippetNotFoundException.class)
                .hasMessageContaining("Snippet not found with ID: " + snippetId);
    }

    @Test
    @DisplayName("Should update snippet when owned by user")
    void updateSnippet_Success() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Snippet existingSnippet = new Snippet(user, "Old Title", "Old Desc", "old code", "java");
        UpdateSnippetRequest request = new UpdateSnippetRequest("New Title", "New Desc", "new code", "kotlin");

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.of(existingSnippet));
        when(snippetRepository.save(existingSnippet)).thenReturn(existingSnippet);

        SnippetResponse response = snippetService.updateSnippet(userId, snippetId, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getDescription()).isEqualTo("New Desc");
        assertThat(response.getCode()).isEqualTo("new code");
        assertThat(response.getLanguage()).isEqualTo("kotlin");
        verify(snippetRepository).save(existingSnippet);
    }

    @Test
    @DisplayName("Should throw SnippetNotFoundException when updating snippet not owned by user")
    void updateSnippet_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();
        UpdateSnippetRequest request = new UpdateSnippetRequest("New Title", "New Desc", "new code", "kotlin");

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snippetService.updateSnippet(userId, snippetId, request))
                .isInstanceOf(SnippetNotFoundException.class);

        verify(snippetRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete snippet when owned by user")
    void deleteSnippet_Success() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Snippet existingSnippet = new Snippet(user, "Title", "Desc", "code", "java");

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.of(existingSnippet));

        snippetService.deleteSnippet(userId, snippetId);

        verify(snippetRepository).delete(existingSnippet);
    }

    @Test
    @DisplayName("Should throw SnippetNotFoundException when deleting snippet not owned by user")
    void deleteSnippet_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID snippetId = UUID.randomUUID();

        when(snippetRepository.findByIdAndUserId(snippetId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> snippetService.deleteSnippet(userId, snippetId))
                .isInstanceOf(SnippetNotFoundException.class);

        verify(snippetRepository, never()).delete(any());
    }
}
