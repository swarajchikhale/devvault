package com.devvault.resource;

import com.devvault.resource.dto.CreateResourceRequest;
import com.devvault.resource.dto.ResourceResponse;
import com.devvault.resource.dto.UpdateResourceRequest;
import com.devvault.resource.exception.ResourceNotFoundException;
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
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;

    @Mock
    private UserRepository userRepository;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService = new ResourceService(resourceRepository, userRepository);
    }

    @Test
    @DisplayName("Should create resource successfully for authenticated user")
    void createResource_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        CreateResourceRequest request = new CreateResourceRequest("Spring Boot Docs", "https://spring.io/projects/spring-boot", "Official docs", "Documentation");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(resourceRepository.save(any(Resource.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResourceResponse response = resourceService.createResource(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Spring Boot Docs");
        assertThat(response.getUrl()).isEqualTo("https://spring.io/projects/spring-boot");
        assertThat(response.getDescription()).isEqualTo("Official docs");
        assertThat(response.getCategory()).isEqualTo("Documentation");

        ArgumentCaptor<Resource> resourceCaptor = ArgumentCaptor.forClass(Resource.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("Should retrieve all resources for authenticated user")
    void getResources_Success() {
        UUID userId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Resource res1 = new Resource(user, "Title 1", "https://example.com/1", "Desc 1", "Cat 1");
        Resource res2 = new Resource(user, "Title 2", "https://example.com/2", "Desc 2", "Cat 2");

        when(resourceRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(res1, res2));

        List<ResourceResponse> responses = resourceService.getResources(userId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getTitle()).isEqualTo("Title 1");
        assertThat(responses.get(1).getTitle()).isEqualTo("Title 2");
    }

    @Test
    @DisplayName("Should retrieve resource by ID when owned by user")
    void getResourceById_Success() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Resource resource = new Resource(user, "Title", "https://example.com", "Desc", "Cat");

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.of(resource));

        ResourceResponse response = resourceService.getResourceById(userId, resourceId);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Title");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when retrieving resource not owned by user")
    void getResourceById_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.getResourceById(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Resource not found with ID: " + resourceId);
    }

    @Test
    @DisplayName("Should update resource when owned by user")
    void updateResource_Success() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Resource existingResource = new Resource(user, "Old Title", "https://old.com", "Old Desc", "Old Cat");
        UpdateResourceRequest request = new UpdateResourceRequest("New Title", "https://new.com", "New Desc", "New Cat");

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.of(existingResource));
        when(resourceRepository.save(existingResource)).thenReturn(existingResource);

        ResourceResponse response = resourceService.updateResource(userId, resourceId, request);

        assertThat(response.getTitle()).isEqualTo("New Title");
        assertThat(response.getUrl()).isEqualTo("https://new.com");
        assertThat(response.getDescription()).isEqualTo("New Desc");
        assertThat(response.getCategory()).isEqualTo("New Cat");
        verify(resourceRepository).save(existingResource);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating resource not owned by user")
    void updateResource_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UpdateResourceRequest request = new UpdateResourceRequest("New Title", "https://new.com", "New Desc", "New Cat");

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.updateResource(userId, resourceId, request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resourceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should delete resource when owned by user")
    void deleteResource_Success() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        User user = new User("johndoe", "john@example.com", "hash");
        Resource existingResource = new Resource(user, "Title", "https://example.com", "Desc", "Cat");

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.of(existingResource));

        resourceService.deleteResource(userId, resourceId);

        verify(resourceRepository).delete(existingResource);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting resource not owned by user")
    void deleteResource_NotFound_ThrowsException() {
        UUID userId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        when(resourceRepository.findByIdAndUserId(resourceId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resourceService.deleteResource(userId, resourceId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(resourceRepository, never()).delete(any());
    }
}
