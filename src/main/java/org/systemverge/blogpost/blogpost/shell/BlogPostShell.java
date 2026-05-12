package org.systemverge.blogpost.blogpost.shell;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.service.BlogPostService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spring Shell CLI commands for the blog-post agent.
 *
 * <p>All business logic is delegated to {@link BlogPostService} —
 * this class is purely a presentation/input layer.</p>
 */
@ShellComponent
public class BlogPostShell {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final BlogPostService blogPostService;

    public BlogPostShell(BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
    }

    /**
     * Generates a reviewed blog post and persists it to the database.
     *
     * <p>Usage: {@code blog --topic "Spring Boot 4 new features"}</p>
     */
    @ShellMethod(key = "blog", value = "Generate a reviewed blog post about a given topic (saved to DB + file)")
    public String generateBlogPost(
            @ShellOption(help = "The topic for the blog post") String topic
    ) {
        BlogPostEntity saved = blogPostService.generateAndPersist(topic);
        return formatPost(saved);
    }

    /**
     * Lists the most recent blog posts from the database (paginated).
     *
     * <p>Usage: {@code list-posts} or {@code list-posts --page 1 --size 5}</p>
     */
    @ShellMethod(key = "list-posts", value = "List recent blog posts stored in the database")
    public String listPosts(
            @ShellOption(defaultValue = "0", help = "Page number (0-based)") int page,
            @ShellOption(defaultValue = "5",  help = "Number of posts per page") int size
    ) {
        Page<BlogPostEntity> result = blogPostService.findAll(PageRequest.of(page, size));
        if (result.isEmpty()) {
            return "No blog posts found. Run 'blog --topic <topic>' to generate one.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Page %d of %d  (%d total posts)%n",
                result.getNumber() + 1, result.getTotalPages(), result.getTotalElements()));
        sb.append("─".repeat(80)).append("\n");

        for (BlogPostEntity post : result) {
            sb.append(String.format("[%s]  %-50s  %s%n",
                    post.getId().toString().substring(0, 8),
                    truncate(post.getTitle(), 50),
                    DATE_FMT.format(post.getCreatedAt())));
        }
        return sb.toString().trim();
    }

    /**
     * Searches blog posts by topic keyword.
     *
     * <p>Usage: {@code search-posts --keyword "docker"}</p>
     */
    @ShellMethod(key = "search-posts", value = "Search blog posts by topic keyword")
    public String searchPosts(
            @ShellOption(help = "Keyword to search in topics") String keyword
    ) {
        List<BlogPostEntity> results = blogPostService.search(keyword);
        if (results.isEmpty()) {
            return "No posts found matching keyword: " + keyword;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d post(s) matching '%s':%n", results.size(), keyword));
        sb.append("─".repeat(80)).append("\n");
        for (BlogPostEntity post : results) {
            sb.append(String.format("[%s]  %-50s  topic: %s%n",
                    post.getId().toString().substring(0, 8),
                    truncate(post.getTitle(), 50),
                    post.getTopic()));
        }
        return sb.toString().trim();
    }

    // ── Private helpers ───────────────────────────────────────────────────

    private String formatPost(BlogPostEntity post) {
        return String.format("""
                ✅ Blog post generated and saved to database!
                
                ID:       %s
                Title:    %s
                Status:   %s
                Created:  %s
                
                %s
                
                ---
                Editor's Notes: %s
                """,
                post.getId(),
                post.getTitle(),
                post.getStatus(),
                DATE_FMT.format(post.getCreatedAt()),
                post.getContent(),
                post.getFeedback()
        ).trim();
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }
}
