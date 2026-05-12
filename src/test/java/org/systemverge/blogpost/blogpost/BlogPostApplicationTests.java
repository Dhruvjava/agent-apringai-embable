package org.systemverge.blogpost.blogpost;

import com.embabel.agent.core.AgentPlatform;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;
import org.systemverge.blogpost.blogpost.repository.BlogPostRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: verifies JPA schema creation and repository queries
 * against H2 with the minimal slice of the Spring context.
 *
 * <p>All Spring Shell and Embabel platform auto-configurations are excluded
 * because they require a live terminal / LLM API key.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.autoconfigure.exclude=" +
                // ── Spring Shell (all configs — avoids JLine/Terminal dependency) ──
                "org.springframework.shell.boot.SpringShellAutoConfiguration," +
                "org.springframework.shell.boot.JLineAutoConfiguration," +
                "org.springframework.shell.boot.JLineShellAutoConfiguration," +
                "org.springframework.shell.boot.LineReaderAutoConfiguration," +
                "org.springframework.shell.boot.ShellRunnerAutoConfiguration," +
                "org.springframework.shell.boot.ShellContextAutoConfiguration," +
                "org.springframework.shell.boot.CommandCatalogAutoConfiguration," +
                "org.springframework.shell.boot.ApplicationRunnerAutoConfiguration," +
                "org.springframework.shell.boot.CompleterAutoConfiguration," +
                "org.springframework.shell.boot.ParameterResolverAutoConfiguration," +
                "org.springframework.shell.boot.StandardAPIAutoConfiguration," +
                "org.springframework.shell.boot.StandardCommandsAutoConfiguration," +
                "org.springframework.shell.boot.ComponentFlowAutoConfiguration," +
                "org.springframework.shell.boot.ExitCodeAutoConfiguration," +
                "org.springframework.shell.boot.TerminalUIAutoConfiguration," +
                "org.springframework.shell.boot.ThemingAutoConfiguration," +
                "org.springframework.shell.boot.UserConfigAutoConfiguration," +
                // ── Embabel (requires live LLM) ───────────────────────────────────
                "com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration," +
                "com.embabel.agent.autoconfigure.shell.AgentShellAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.openai.AgentOpenAiAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.anthropic.AgentAnthropicAutoConfiguration"
})
@Transactional
class BlogPostApplicationTests {

    /** Satisfies BlogPostServiceImpl without real Embabel platform. */
    @MockitoBean
    AgentPlatform agentPlatform;

    @Autowired
    BlogPostRepository repository;

    // ── Schema + CRUD ─────────────────────────────────────────────────────

    @Test
    void contextLoads_schemaCreated_andCrudWorks() {
        BlogPostEntity saved = repository.saveAndFlush(BlogPostEntity.builder()
                .topic("Spring Boot 4 integration test")
                .title("Context Load Post")
                .content("# Intro\n\nVerifies H2 schema and CRUD.")
                .feedback("Auto-generated.")
                .status(BlogPostStatus.REVIEWED)
                .build());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(BlogPostStatus.REVIEWED);
    }

    // ── Repository query methods ──────────────────────────────────────────

    @Test
    void repositoryQuery_findByStatus_returnsCorrectPosts() {
        repository.save(BlogPostEntity.builder().topic("r").title("Reviewed").content("C A")
                .status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder().topic("d").title("Draft").content("C B")
                .status(BlogPostStatus.DRAFT).build());
        repository.flush();

        List<BlogPostEntity> reviewed = repository.findByStatusOrderByCreatedAtDesc(BlogPostStatus.REVIEWED);
        List<BlogPostEntity> drafts   = repository.findByStatusOrderByCreatedAtDesc(BlogPostStatus.DRAFT);

        assertThat(reviewed).hasSize(1).allMatch(p -> p.getStatus() == BlogPostStatus.REVIEWED);
        assertThat(drafts).hasSize(1).allMatch(p -> p.getStatus() == BlogPostStatus.DRAFT);
    }

    @Test
    void repositoryQuery_searchByTopicKeyword_isCaseInsensitive() {
        repository.save(BlogPostEntity.builder().topic("Docker Basics").title("Docker Guide")
                .content("C").status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder().topic("Kubernetes").title("K8s Guide")
                .content("C").status(BlogPostStatus.REVIEWED).build());
        repository.flush();

        List<BlogPostEntity> results =
                repository.findByTopicContainingIgnoreCaseOrderByCreatedAtDesc("docker");

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getTitle()).isEqualTo("Docker Guide");
    }

    @Test
    void repositoryQuery_findAllPaginated_returnsAllPosts() {
        repository.save(BlogPostEntity.builder().topic("p1").title("Post 1").content("C 1")
                .status(BlogPostStatus.REVIEWED).build());
        repository.save(BlogPostEntity.builder().topic("p2").title("Post 2").content("C 2")
                .status(BlogPostStatus.REVIEWED).build());
        repository.flush();

        Page<BlogPostEntity> page = repository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
