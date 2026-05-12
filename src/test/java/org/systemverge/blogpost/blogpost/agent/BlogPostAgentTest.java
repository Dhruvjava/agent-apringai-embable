package org.systemverge.blogpost.blogpost.agent;

import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.test.unit.FakeOperationContext;
import com.embabel.agent.test.unit.FakePromptRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BlogPostAgentTest {

    private static final int WORD_COUNT = 800;
    private static final int REVIEW_WORD_COUNT = 200;
    private static final String OUTPUT_DIR = "target/test-blog-posts";

    private BlogPostAgent agent;
    private FakeOperationContext context;
    private FakePromptRunner promptRunner;

    @BeforeEach
    void setUp() {
        agent = new BlogPostAgent(WORD_COUNT, REVIEW_WORD_COUNT, OUTPUT_DIR);
        context = FakeOperationContext.create();
        promptRunner = (FakePromptRunner) context.promptRunner();
    }

    @Test
    @DisplayName("writeDraft should include the user topic in the prompt")
    void writeDraftIncludesUserTopicInPrompt() {
        var draft = new BlogPostAgent.BlogDraft("Spring Boot Basics", "# Introduction\nSpring Boot content...");
        context.expectResponse(draft);

        var input = new UserInput("Spring Boot for beginners", Instant.now());
        var result = agent.writeDraft(input, context.ai());

        assertNotNull(result, "Result should not be null");
        assertEquals("Spring Boot Basics", result.title());
        assertEquals("# Introduction\nSpring Boot content...", result.content());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains("Spring Boot for beginners"),
                "Prompt should contain the user's topic");
    }

    @Test
    @DisplayName("writeDraft should include the configured word count in the prompt")
    void writeDraftIncludesWordCountInPrompt() {
        var draft = new BlogPostAgent.BlogDraft("Any Title", "Content...");
        context.expectResponse(draft);

        var input = new UserInput("Any topic", Instant.now());
        agent.writeDraft(input, context.ai());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains(String.valueOf(WORD_COUNT)),
                "Prompt should contain the configured word count");
    }

    @Test
    @DisplayName("writeDraft should ask for Markdown formatting in the prompt")
    void writeDraftRequestsMarkdownFormat() {
        var draft = new BlogPostAgent.BlogDraft("Title", "Content");
        context.expectResponse(draft);

        var input = new UserInput("Test topic", Instant.now());
        agent.writeDraft(input, context.ai());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains("Markdown"),
                "Prompt should mention Markdown formatting");
    }

    @Test
    @DisplayName("reviewAndSave should include draft title and content in the prompt")
    void reviewAndSaveIncludesDraftDetailsInPrompt() {
        var draft = new BlogPostAgent.BlogDraft("Spring Boot Basics", "Draft content about Spring Boot...");
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Spring Boot: A Comprehensive Guide",
                "Improved content about Spring Boot...",
                "Improved title and added practical examples"
        );
        context.expectResponse(reviewed);

        var input = new UserInput("Spring Boot for beginners", Instant.now());
        var result = agent.reviewAndSave(input, draft, context.ai());

        assertNotNull(result, "Result should not be null");
        assertEquals("Spring Boot: A Comprehensive Guide", result.title());
        assertEquals("Improved content about Spring Boot...", result.content());
        assertEquals("Improved title and added practical examples", result.feedback());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains("Spring Boot Basics"),
                "Prompt should contain the original draft title");
        assertTrue(prompt.contains("Draft content about Spring Boot"),
                "Prompt should contain the original draft content");
        assertTrue(prompt.contains("Spring Boot for beginners"),
                "Prompt should contain the original user request");
    }

    @Test
    @DisplayName("reviewAndSave should include review word count in the prompt")
    void reviewAndSaveIncludesReviewWordCountInPrompt() {
        var draft = new BlogPostAgent.BlogDraft("Title", "Content...");
        var reviewed = new BlogPostAgent.ReviewedBlogPost("New Title", "New Content", "Feedback");
        context.expectResponse(reviewed);

        var input = new UserInput("Test topic", Instant.now());
        agent.reviewAndSave(input, draft, context.ai());

        var prompt = promptRunner.getLlmInvocations().getFirst().getMessages().getFirst().getContent();
        assertTrue(prompt.contains(String.valueOf(REVIEW_WORD_COUNT)),
                "Prompt should contain the review word count");
    }

    @Test
    @DisplayName("ReviewedBlogPost getContent should format as Markdown with title as h1")
    void reviewedBlogPostGetContentFormatsCorrectly() {
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Spring Boot Guide",
                "## Introduction\nContent here...",
                "Enhanced clarity and added code examples"
        );

        var content = reviewed.getContent();

        assertTrue(content.contains("# Spring Boot Guide"), "Should contain title as h1 heading");
        assertTrue(content.contains("## Introduction"), "Should contain content sections");
        assertTrue(content.contains("Enhanced clarity and added code examples"), "Should contain feedback");
        assertTrue(content.contains("Editor's Notes:"), "Should contain editor notes label");
    }

    @Test
    @DisplayName("ReviewedBlogPost getTimestamp should return a current non-null timestamp")
    void reviewedBlogPostGetTimestampReturnsCurrentTime() {
        var before = Instant.now();
        var reviewed = new BlogPostAgent.ReviewedBlogPost("Title", "Content", "Feedback");
        var timestamp = reviewed.getTimestamp();
        var after = Instant.now();

        assertNotNull(timestamp, "Timestamp should not be null");
        assertFalse(timestamp.isBefore(before), "Timestamp should not be before test start");
        assertFalse(timestamp.isAfter(after), "Timestamp should not be after test end");
    }

    @Test
    @DisplayName("saveToFile should create a markdown file in the output directory")
    void saveToFileCreatesMarkdownFile() throws Exception {
        var reviewed = new BlogPostAgent.ReviewedBlogPost(
                "Test Blog Post Title",
                "Test content in Markdown",
                "Minor edits made"
        );

        agent.saveToFile(reviewed);

        Path outputPath = Path.of(OUTPUT_DIR);
        assertTrue(Files.exists(outputPath), "Output directory should be created");

        var files = Files.list(outputPath)
                .filter(p -> p.getFileName().toString().startsWith("test-blog-post-title"))
                .toList();
        assertFalse(files.isEmpty(), "A markdown file should have been saved");
        assertTrue(files.getFirst().getFileName().toString().endsWith(".md"),
                "Saved file should have .md extension");
    }

    @Test
    @DisplayName("BlogDraft record should correctly expose title and content")
    void blogDraftRecordFields() {
        var draft = new BlogPostAgent.BlogDraft("My Title", "My Content");

        assertEquals("My Title", draft.title());
        assertEquals("My Content", draft.content());
    }

    @Test
    @DisplayName("ReviewedBlogPost record should correctly expose all fields")
    void reviewedBlogPostRecordFields() {
        var post = new BlogPostAgent.ReviewedBlogPost("Title", "Content", "Feedback");

        assertEquals("Title", post.title());
        assertEquals("Content", post.content());
        assertEquals("Feedback", post.feedback());
    }
}
