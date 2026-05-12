package org.systemverge.blogpost.blogpost.agent;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.unit.FakeOperationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests that verify the complete two-step blog post workflow:
 * writeDraft → reviewAndSave, and file persistence.
 */
class BlogPostAgentIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Full workflow: user topic → draft → reviewed post with correct data flow")
    void shouldExecuteCompleteWorkflow() {
        var agent = new BlogPostAgent(100, 50, tempDir.toString());
        var input = new UserInput("Write about Spring Boot", Instant.now());

        var expectedDraft = new BlogPostAgent.BlogDraft(
                "Spring Boot Essentials",
                "## Getting Started\nSpring Boot makes stand-alone Spring apps easy."
        );
        var expectedReviewed = new BlogPostAgent.ReviewedBlogPost(
                "Spring Boot Essentials: A Complete Guide",
                "## Getting Started\nSpring Boot revolutionises stand-alone Spring app creation.",
                "Improved title clarity and tightened the opening sentence."
        );

        // Step 1: writeDraft
        var draftContext = FakeOperationContext.create();
        draftContext.expectResponse(expectedDraft);
        var actualDraft = agent.writeDraft(input, draftContext.ai());

        assertEquals("Spring Boot Essentials", actualDraft.title(),
                "Draft title should match expected");
        assertTrue(actualDraft.content().contains("Spring Boot"),
                "Draft content should contain the topic");

        // Step 2: reviewAndSave with the actual draft produced in step 1
        var reviewContext = FakeOperationContext.create();
        reviewContext.expectResponse(expectedReviewed);
        var actualReviewed = agent.reviewAndSave(input, actualDraft, reviewContext.ai());

        assertNotNull(actualReviewed, "Reviewed post should not be null");
        assertEquals("Spring Boot Essentials: A Complete Guide", actualReviewed.title(),
                "Reviewed title should match expected");
        assertEquals("Improved title clarity and tightened the opening sentence.", actualReviewed.feedback(),
                "Feedback should be preserved");
    }

    @Test
    @DisplayName("ReviewAndSave prompt must include the draft from writeDraft")
    void reviewPromptShouldContainDraftOutput() {
        var agent = new BlogPostAgent(100, 50, tempDir.toString());
        var input = new UserInput("Kubernetes tutorial", Instant.now());

        var draft = new BlogPostAgent.BlogDraft(
                "Kubernetes for Java Devs",
                "## Introduction\nKubernetes simplifies container orchestration."
        );
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Mastering Kubernetes for Java Developers",
                "## Introduction\nKubernetes transforms how you deploy Java applications.",
                "Added stronger opening and improved heading."
        );

        // Write draft and capture its output
        var draftCtx = FakeOperationContext.create();
        draftCtx.expectResponse(draft);
        var actualDraft = agent.writeDraft(input, draftCtx.ai());

        // Verify the review prompt includes the draft's title and content
        var reviewCtx = FakeOperationContext.create();
        var reviewRunner = (com.embabel.agent.test.unit.FakePromptRunner) reviewCtx.promptRunner();
        reviewCtx.expectResponse(reviewed);
        agent.reviewAndSave(input, actualDraft, reviewCtx.ai());

        var reviewPrompt = reviewRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(reviewPrompt.contains("Kubernetes for Java Devs"),
                "Review prompt should include the draft title from step 1");
        assertTrue(reviewPrompt.contains("Kubernetes simplifies container orchestration"),
                "Review prompt should include the draft content from step 1");
        assertTrue(reviewPrompt.contains("Kubernetes tutorial"),
                "Review prompt should include the original user request");
    }

    @Test
    @DisplayName("Reviewed post is persisted as a Markdown file after reviewAndSave")
    void shouldSaveMarkdownFileToDisk() throws IOException {
        var agent = new BlogPostAgent(100, 50, tempDir.toString());
        var input = new UserInput("Clean Code principles", Instant.now());

        var draft = new BlogPostAgent.BlogDraft("Clean Code", "Write clean code always.");
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Clean Code Principles Every Developer Should Know",
                "## Introduction\nClean code is readable, maintainable code.",
                "Expanded title and improved intro."
        );

        var draftCtx = FakeOperationContext.create();
        draftCtx.expectResponse(draft);
        var actualDraft = agent.writeDraft(input, draftCtx.ai());

        var reviewCtx = FakeOperationContext.create();
        reviewCtx.expectResponse(reviewed);
        agent.reviewAndSave(input, actualDraft, reviewCtx.ai());

        List<Path> mdFiles = Files.list(tempDir)
                .filter(p -> p.getFileName().toString().endsWith(".md"))
                .toList();
        assertFalse(mdFiles.isEmpty(), "At least one .md file should be saved");

        String fileContent = Files.readString(mdFiles.getFirst());
        assertTrue(fileContent.contains("# Clean Code Principles Every Developer Should Know"),
                "File should contain the reviewed post title");
        assertTrue(fileContent.contains("Editor's Notes:"),
                "File should contain the editor notes section");
        assertTrue(fileContent.contains("Expanded title and improved intro."),
                "File should contain the feedback text");
    }

    @Test
    @DisplayName("getContent output contains all required sections")
    void getContentShouldContainAllSections() {
        var post = new BlogPostAgent.ReviewedBlogPost(
                "My Blog Title",
                "## Section One\nContent here.\n## Section Two\nMore content.",
                "Fixed headings and tightened prose."
        );

        var content = post.getContent();

        assertAll("All content sections must be present",
                () -> assertTrue(content.contains("# My Blog Title"), "Should have h1 title"),
                () -> assertTrue(content.contains("## Section One"), "Should have section headings"),
                () -> assertTrue(content.contains("Content here."), "Should have body content"),
                () -> assertTrue(content.contains("Editor's Notes:"), "Should have editor notes label"),
                () -> assertTrue(content.contains("Fixed headings and tightened prose."), "Should have feedback"),
                () -> assertTrue(content.contains("Generated on"), "Should have generation date")
        );
    }

    @Test
    @DisplayName("Multiple topics produce independently reviewed posts")
    void multipleDifferentTopicsShouldProduceIndependentResults() {
        var agent = new BlogPostAgent(100, 50, tempDir.toString());

        var topicA = new UserInput("Docker basics", Instant.now());
        var topicB = new UserInput("Git workflows", Instant.now());

        var draftA = new BlogPostAgent.BlogDraft("Docker Basics", "Docker content...");
        var draftB = new BlogPostAgent.BlogDraft("Git Workflows", "Git content...");
        var reviewedA = new BlogPostAgent.ReviewedBlogPost("Docker Basics Guide", "Improved Docker content.", "Minor edits.");
        var reviewedB = new BlogPostAgent.ReviewedBlogPost("Git Workflows Explained", "Improved Git content.", "Restructured sections.");

        var ctxA = FakeOperationContext.create();
        ctxA.expectResponse(draftA);
        var actualDraftA = agent.writeDraft(topicA, ctxA.ai());

        var ctxB = FakeOperationContext.create();
        ctxB.expectResponse(draftB);
        var actualDraftB = agent.writeDraft(topicB, ctxB.ai());

        var revCtxA = FakeOperationContext.create();
        revCtxA.expectResponse(reviewedA);
        var resultA = agent.reviewAndSave(topicA, actualDraftA, revCtxA.ai());

        var revCtxB = FakeOperationContext.create();
        revCtxB.expectResponse(reviewedB);
        var resultB = agent.reviewAndSave(topicB, actualDraftB, revCtxB.ai());

        assertNotEquals(resultA.title(), resultB.title(), "Different topics should produce different titles");
        assertNotEquals(resultA.content(), resultB.content(), "Different topics should produce different content");
    }
}
