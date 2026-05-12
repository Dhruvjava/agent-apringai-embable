package org.systemverge.blogpost.blogpost;

import com.embabel.agent.core.AgentPlatform;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Smoke test — verifies the Spring application context loads successfully,
 * including JPA schema creation, repository beans, service beans, and REST controller.
 *
 * <p>The three Embabel auto-configurations are excluded because:
 * <ul>
 *   <li>{@code AgentPlatformAutoConfiguration} — requires a registered LLM model
 *       (gpt-4.1-mini) which is unavailable in CI/test environments with no API keys.</li>
 *   <li>{@code AgentOpenAiAutoConfiguration} / {@code AgentAnthropicAutoConfiguration} —
 *       compiled against Spring Framework 6.x while Spring Boot 4 ships Spring Framework 7.x,
 *       causing {@code NoSuchMethodError} on {@code HttpHeaders}.</li>
 * </ul>
 * {@code AgentPlatform} is provided as a Mockito bean so {@code BlogPostServiceImpl}
 * can be wired without a real LLM infrastructure.
 * All agent logic is thoroughly covered in {@code BlogPostAgentTest} and
 * {@code BlogPostAgentIntegrationTest} which use Embabel's own {@code FakeOperationContext}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.shell.interactive.enabled=false",
        "spring.autoconfigure.exclude=" +
                "com.embabel.agent.autoconfigure.platform.AgentPlatformAutoConfiguration," +
                "com.embabel.agent.autoconfigure.shell.AgentShellAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.openai.AgentOpenAiAutoConfiguration," +
                "com.embabel.agent.autoconfigure.models.anthropic.AgentAnthropicAutoConfiguration"
})
class BlogPostApplicationTests {

    /**
     * AgentPlatform is the dependency of BlogPostServiceImpl.
     * Since Embabel platform auto-config is excluded, we provide a mock so
     * the service bean can be created and the full context wires correctly.
     */
    @MockitoBean
    AgentPlatform agentPlatform;

    @Test
    void contextLoads() {
        // Passes if the Spring context starts without errors:
        // H2 schema, JPA repositories, service layer, and REST controller are all validated.
    }
}
