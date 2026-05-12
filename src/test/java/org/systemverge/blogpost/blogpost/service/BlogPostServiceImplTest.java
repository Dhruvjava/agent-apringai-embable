package org.systemverge.blogpost.blogpost.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.systemverge.blogpost.blogpost.agent.BlogPostAgent;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;
import org.systemverge.blogpost.blogpost.repository.BlogPostRepository;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BlogPostServiceImpl}.
 *
 * <p>The Embabel {@code AgentInvocation} static call is isolated via a subclass seam:
 * {@link TestableBlogPostServiceImpl} overrides {@code invokeAgent()} so no live LLM
 * or static mock framework is needed.</p>
 */
@ExtendWith(MockitoExtension.class)
class BlogPostServiceImplTest {

    @Mock
    private BlogPostRepository repository;

    /**
     * Subclass that bypasses the real Embabel agent call.
     * This is the Seam pattern — overriding the protected hook for tests.
     */
    static class TestableBlogPostServiceImpl extends BlogPostServiceImpl {
        private BlogPostAgent.ReviewedBlogPost stubbedResult;

        TestableBlogPostServiceImpl(BlogPostRepository repository) {
            super(null, repository); // agentPlatform not needed; invokeAgent is overridden
        }

        void stubAgentResult(BlogPostAgent.ReviewedBlogPost result) {
            this.stubbedResult = result;
        }

        @Override
        protected BlogPostAgent.ReviewedBlogPost invokeAgent(String topic) {
            return stubbedResult;
        }
    }

    private TestableBlogPostServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TestableBlogPostServiceImpl(repository);
    }

    // ── generateAndPersist ────────────────────────────────────────────────

    @Test
    @DisplayName("generateAndPersist should build entity from agent result and save it")
    void shouldGenerateAndPersistBlogPost() {
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Spring Boot 4 Guide",
                "## Intro\nContent here.",
                "Improved title and structure."
        );
        service.stubAgentResult(reviewed);

        var savedEntity = BlogPostEntity.builder()
                .topic("spring boot 4")
                .title("Spring Boot 4 Guide")
                .content("## Intro\nContent here.")
                .feedback("Improved title and structure.")
                .status(BlogPostStatus.REVIEWED)
                .build();
        given(repository.save(any(BlogPostEntity.class))).willReturn(savedEntity);

        BlogPostEntity result = service.generateAndPersist("spring boot 4");

        // Verify what was actually passed to save()
        ArgumentCaptor<BlogPostEntity> captor = ArgumentCaptor.forClass(BlogPostEntity.class);
        verify(repository).save(captor.capture());
        BlogPostEntity captured = captor.getValue();

        assertThat(captured.getTopic()).isEqualTo("spring boot 4");
        assertThat(captured.getTitle()).isEqualTo("Spring Boot 4 Guide");
        assertThat(captured.getContent()).isEqualTo("## Intro\nContent here.");
        assertThat(captured.getFeedback()).isEqualTo("Improved title and structure.");
        assertThat(captured.getStatus()).isEqualTo(BlogPostStatus.REVIEWED);
        assertThat(result).isSameAs(savedEntity);
    }

    @Test
    @DisplayName("generateAndPersist should propagate the topic as-is to the entity")
    void shouldPreserveTopicInEntity() {
        var reviewed = new BlogPostAgent.ReviewedBlogPost("Title", "Content", "Feedback");
        service.stubAgentResult(reviewed);

        var built = BlogPostEntity.builder()
                .topic("Docker best practices")
                .title("Title").content("Content").feedback("Feedback").build();
        given(repository.save(any())).willReturn(built);

        service.generateAndPersist("Docker best practices");

        ArgumentCaptor<BlogPostEntity> captor = ArgumentCaptor.forClass(BlogPostEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getTopic()).isEqualTo("Docker best practices");
    }

    // ── findAll ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findAll should delegate to repository with given pageable")
    void shouldFindAllPaginated() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BlogPostEntity> expected = new PageImpl<>(List.of());
        given(repository.findAllByOrderByCreatedAtDesc(pageable)).willReturn(expected);

        Page<BlogPostEntity> result = service.findAll(pageable);

        assertThat(result).isSameAs(expected);
        verify(repository).findAllByOrderByCreatedAtDesc(pageable);
    }

    // ── findById ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById should return present when entity exists")
    void shouldReturnEntityWhenFound() {
        UUID id = UUID.randomUUID();
        var entity = BlogPostEntity.builder()
                .topic("test topic").title("Test").content("Content").build();
        given(repository.findById(id)).willReturn(Optional.of(entity));

        assertThat(service.findById(id)).isPresent().contains(entity);
    }

    @Test
    @DisplayName("findById should return empty when entity does not exist")
    void shouldReturnEmptyWhenNotFound() {
        UUID id = UUID.randomUUID();
        given(repository.findById(id)).willReturn(Optional.empty());

        assertThat(service.findById(id)).isEmpty();
    }

    // ── findByStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("findByStatus should delegate to repository with correct status")
    void shouldFindByStatus() {
        List<BlogPostEntity> expected = List.of();
        given(repository.findByStatusOrderByCreatedAtDesc(BlogPostStatus.REVIEWED)).willReturn(expected);

        List<BlogPostEntity> result = service.findByStatus(BlogPostStatus.REVIEWED);

        assertThat(result).isSameAs(expected);
        verify(repository).findByStatusOrderByCreatedAtDesc(BlogPostStatus.REVIEWED);
    }

    // ── search ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search should delegate keyword to repository")
    void shouldSearchByKeyword() {
        List<BlogPostEntity> expected = List.of();
        given(repository.findByTopicContainingIgnoreCaseOrderByCreatedAtDesc("docker")).willReturn(expected);

        List<BlogPostEntity> result = service.search("docker");

        assertThat(result).isSameAs(expected);
        verify(repository).findByTopicContainingIgnoreCaseOrderByCreatedAtDesc("docker");
    }

    // ── deleteById ────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteById should call repository.deleteById when entity exists")
    void shouldDeleteWhenExists() {
        UUID id = UUID.randomUUID();
        given(repository.existsById(id)).willReturn(true);

        service.deleteById(id);

        verify(repository).deleteById(id);
    }

    @Test
    @DisplayName("deleteById should throw EntityNotFoundException when entity does not exist")
    void shouldThrowWhenDeletingNonExistentPost() {
        UUID id = UUID.randomUUID();
        given(repository.existsById(id)).willReturn(false);

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(repository, never()).deleteById(any());
    }

    // ── BlogPostEntity.Builder ────────────────────────────────────────────

    @Test
    @DisplayName("BlogPostEntity.Builder should throw when required field topic is missing")
    void builderShouldRejectMissingTopic() {
        assertThatThrownBy(() -> BlogPostEntity.builder()
                .title("Title").content("Content").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("topic");
    }

    @Test
    @DisplayName("BlogPostEntity.Builder should throw when required field title is missing")
    void builderShouldRejectMissingTitle() {
        assertThatThrownBy(() -> BlogPostEntity.builder()
                .topic("test").content("Content").build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("title");
    }

    @Test
    @DisplayName("BlogPostEntity.Builder should default status to REVIEWED")
    void builderShouldDefaultToReviewedStatus() {
        BlogPostEntity entity = BlogPostEntity.builder()
                .topic("test").title("Test Title").content("Content").build();

        assertThat(entity.getStatus()).isEqualTo(BlogPostStatus.REVIEWED);
    }

    @Test
    @DisplayName("BlogPostEntity.Builder should allow setting DRAFT status")
    void builderShouldAllowDraftStatus() {
        BlogPostEntity entity = BlogPostEntity.builder()
                .topic("test").title("Title").content("Content")
                .status(BlogPostStatus.DRAFT)
                .build();

        assertThat(entity.getStatus()).isEqualTo(BlogPostStatus.DRAFT);
    }
}
