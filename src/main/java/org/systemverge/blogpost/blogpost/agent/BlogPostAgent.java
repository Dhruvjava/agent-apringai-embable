package org.systemverge.blogpost.blogpost.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.annotation.Export;
import com.embabel.agent.api.common.Ai;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.domain.library.HasContent;
import com.embabel.agent.prompt.persona.RoleGoalBackstory;
import com.embabel.common.ai.model.LlmOptions;
import com.embabel.common.core.types.Timestamped;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

abstract class Personas {

    static final RoleGoalBackstory WRITER = new RoleGoalBackstory(
            "Expert Technical Writer",
            "Write informative, engaging, and well-structured blog posts",
            "Seasoned writer with 10+ years in tech blogging and content creation"
    );

    static final RoleGoalBackstory REVIEWER = new RoleGoalBackstory(
            "Content Editor and SEO Specialist",
            "Review and improve blog posts for clarity, accuracy, and engagement",
            "Experienced editor who has reviewed thousands of articles for top tech publications"
    );
}

@Agent(description = "Generates and reviews high-quality blog posts on any topic")
public class BlogPostAgent {

    private static final Logger log = LoggerFactory.getLogger(BlogPostAgent.class);

    @JsonClassDescription("A blog post draft with a title and full content in Markdown")
    public record BlogDraft(
            @JsonPropertyDescription("The SEO-friendly title of the blog post") String title,
            @JsonPropertyDescription("The full blog post content formatted in Markdown") String content
    ) {}

    @JsonClassDescription("A reviewed and polished blog post ready for publication")
    public record ReviewedBlogPost(
            @JsonPropertyDescription("The refined, compelling title of the blog post") String title,
            @JsonPropertyDescription("The improved and polished blog post content in Markdown") String content,
            @JsonPropertyDescription("Brief editorial feedback summarising what was improved and why") String feedback
    ) implements HasContent, Timestamped {

        @Override
        public Instant getTimestamp() {
            return Instant.now();
        }

        @Override
        public String getContent() {
            return String.format("""
                    # %s

                    %s

                    ---
                    **Editor's Notes:** %s

                    *Generated on %s*
                    """,
                    title(),
                    content(),
                    feedback(),
                    getTimestamp().atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy"))
            ).trim();
        }
    }

    private final int wordCount;
    private final int reviewWordCount;
    private final String outputDir;

    public BlogPostAgent(
            @Value("${blog-post.word-count:800}") int wordCount,
            @Value("${blog-post.review-word-count:200}") int reviewWordCount,
            @Value("${blog-post.output-dir:blog-posts}") String outputDir
    ) {
        this.wordCount = wordCount;
        this.reviewWordCount = reviewWordCount;
        this.outputDir = outputDir;
    }

    @Action(description = "Write an initial draft of the blog post based on the requested topic")
    public BlogDraft writeDraft(UserInput userInput, Ai ai) {
        return ai
                .withLlm(LlmOptions.withAutoLlm().withTemperature(0.7))
                .withPromptContributor(Personas.WRITER)
                .creating(BlogDraft.class)
                .fromPrompt(String.format("""
                        Write a comprehensive blog post about the following topic.
                        The post should be approximately %d words.
                        Include an engaging introduction, well-organised sections with Markdown headings,
                        practical examples where applicable, and a strong conclusion.
                        Format the entire content in Markdown.

                        Topic: %s
                        """,
                        wordCount,
                        userInput.getContent()
                ).trim());
    }

    @AchievesGoal(
            description = "A reviewed and polished blog post has been generated and saved",
            export = @Export(remote = true, name = "generateBlogPost"))
    @Action
    public ReviewedBlogPost reviewAndSave(UserInput userInput, BlogDraft draft, Ai ai) {
        var result = ai
                .withAutoLlm()
                .withPromptContributor(Personas.REVIEWER)
                .creating(ReviewedBlogPost.class)
                .fromPrompt(String.format("""
                        Review and improve the following blog post draft.
                        Provide:
                        - A refined title (keep it compelling and SEO-friendly)
                        - Improved content (approximately %d words, in Markdown)
                        - Brief editorial feedback on what you changed and why

                        The original request was about: %s

                        # Original Title
                        %s

                        # Original Content
                        %s
                        """,
                        reviewWordCount,
                        userInput.getContent(),
                        draft.title(),
                        draft.content()
                ).trim());

        saveToFile(result);
        return result;
    }

    void saveToFile(ReviewedBlogPost post) {
        try {
            Path dir = Path.of(outputDir);
            Files.createDirectories(dir);

            String safeName = post.title()
                    .toLowerCase()
                    .replaceAll("[^a-z0-9\\s-]", "")
                    .replaceAll("\\s+", "-")
                    .replaceAll("-+", "-");
            if (safeName.length() > 50) {
                safeName = safeName.substring(0, 50);
            }
            String filename = safeName + "-" + System.currentTimeMillis() + ".md";
            Path filePath = dir.resolve(filename);
            Files.writeString(filePath, post.getContent());
            log.info("Blog post saved to: {}", filePath.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save blog post to file", e);
        }
    }
}
