package org.systemverge.blogpost.blogpost;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import com.embabel.agent.core.AgentPlatform;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;
import org.systemverge.blogpost.blogpost.repository.BlogPostRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies:
 * <ol>
 *   <li>The full Spring context loads (JPA, repositories, service, REST controller).</li>
 *   <li>The H2 schema is correctly created from the {@link BlogPostEntity} mapping.</li>
 *   <li>All three custom repository query methods work correctly.</li>
 * </ol>
 *
 * <p>Embabel platform, shell, and model auto-configurations are excluded because
 * they require a live LLM API key — not available in CI. {@code AgentPlatform} is
 * provided as a {@code @MockitoBean} to satisfy {@code BlogPostServiceImpl}'s constructor.</p>
 *
 * <p>{@code webEnvironment = NONE} prevents the HTTP server from starting (faster,
 * and avoids shell blocking the test thread).</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.autoconfigure.exclude=" +
                "com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration," +
                "com.embabel.agent.autoconfigure.shell.AgentShellAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.openai.AgentOpenAiAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.anthropic.AgentAnthropicAutoConfiguration"
})
@Transactional
class BlogPostApplicationTests {

    /** Mock satisfies BlogPostServiceImpl constructor without real Embabel platform. */
    @MockitoBean
    AgentPlatform agentPlatform;

    @Autowired
    BlogPostRepository repository;

    @Test
    void contextLoads_schemaCreated_andBasicCrudWorks() {
        BlogPostEntity entity = BlogPostEntity.builder()
                .topic("Spring Boot 4 context load test")
                .title("Context Load Validation Post")
                .content("# Intro\n\nVerifies H2 schema creation and CRUD.")
                .feedback("Auto-generated.")
                .status(BlogPostStatus.REVIEWED)
                .build();

        BlogPostEntity saved = repository.save(entity);
        repository.flush();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BlogPostStatus.REVIEWED);
    }

    @Test
    void repositoryQuery_findByStatus_returnsMatchingPosts() {
        repository.save(BlogPostEntity.builder()
                .topic("reviewed-topic").title("Reviewed Post").content("Content A")
                .status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder()
                .topic("draft-topic").title("Draft Post").content("Content B")
                .status(BlogPostStatus.DRAFT).build());
        repository.flush();

        List<BlogPostEntity> reviewed = repository.findByStatusOrderByCreatedAtDesc(BlogPostStatus.REVIEWED);
        List<BlogPostEntity> drafts   = repository.findByStatusOrderByCreatedAtDesc(BlogPostStatus.DRAFT);

        assertThat(reviewed).hasSize(1).allMatch(p -> p.getStatus() == BlogPostStatus.REVIEWED);
        assertThat(drafts).hasSize(1).allMatch(p -> p.getStatus() == BlogPostStatus.DRAFT);
    }

    @Test
    void repositoryQuery_searchByTopicKeyword_isCaseInsensitive() {
        repository.save(BlogPostEntity.builder()
                .topic("Docker Basics and Best Practices").title("Docker Guide").content("Content")
                .status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder()
                .topic("Kubernetes Orchestration").title("K8s Guide").content("Content")
                .status(BlogPostStatus.REVIEWED).build());
        repository.flush();

        List<BlogPostEntity> results =
                repository.findByTopicContainingIgnoreCaseOrderByCreatedAtDesc("docker");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Docker Guide");
    }

    @Test
    void repositoryQuery_findAllPaginated_returnsCorrectCount() {
        repository.save(BlogPostEntity.builder()
                .topic("post-1").title("First Post").content("Content 1")
                .status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder()
                .topic("post-2").title("Second Post").content("Content 2")
                .status(BlogPostStatus.REVIEWED).build());
        repository.flush();

        Page<BlogPostEntity> page = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }
}
