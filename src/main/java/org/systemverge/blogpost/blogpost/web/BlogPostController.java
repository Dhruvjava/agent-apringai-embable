package org.systemverge.blogpost.blogpost.web;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;
import org.systemverge.blogpost.blogpost.service.BlogPostService;
import org.systemverge.blogpost.blogpost.web.dto.GenerateBlogPostRequest;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing CRUD and generation endpoints for blog posts.
 *
 * <p>Base path: {@code /api/v1/blog-posts}</p>
 *
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Description</th></tr>
 *   <tr><td>POST</td><td>/generate</td><td>Run AI agent, persist, return post</td></tr>
 *   <tr><td>GET</td><td>/</td><td>Paginated list (newest first)</td></tr>
 *   <tr><td>GET</td><td>/{id}</td><td>Single post by UUID</td></tr>
 *   <tr><td>GET</td><td>/status/{status}</td><td>Filter by status</td></tr>
 *   <tr><td>GET</td><td>/search?q=keyword</td><td>Topic keyword search</td></tr>
 *   <tr><td>DELETE</td><td>/{id}</td><td>Delete a post</td></tr>
 * </table>
 */
@RestController
@RequestMapping("/api/v1/blog-posts")
public class BlogPostController {

    private static final Logger log = LoggerFactory.getLogger(BlogPostController.class);

    private final BlogPostService blogPostService;

    public BlogPostController(BlogPostService blogPostService) {
        this.blogPostService = blogPostService;
    }

    // ── Generate ──────────────────────────────────────────────────────────

    /**
     * Triggers the full AI agent pipeline and persists the result.
     * This is a potentially slow operation (multiple LLM calls).
     */
    @PostMapping("/generate")
    public ResponseEntity<BlogPostEntity> generate(@Valid @RequestBody GenerateBlogPostRequest request) {
        log.info("REST: generate blog post for topic='{}'", request.topic());
        BlogPostEntity saved = blogPostService.generateAndPersist(request.topic());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // ── Query ─────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<Page<BlogPostEntity>> findAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        return ResponseEntity.ok(blogPostService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BlogPostEntity> findById(@PathVariable UUID id) {
        return blogPostService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<BlogPostEntity>> findByStatus(@PathVariable BlogPostStatus status) {
        return ResponseEntity.ok(blogPostService.findByStatus(status));
    }

    @GetMapping("/search")
    public ResponseEntity<List<BlogPostEntity>> search(@RequestParam("q") String keyword) {
        return ResponseEntity.ok(blogPostService.search(keyword));
    }

    // ── Delete ────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        blogPostService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── Exception handlers ────────────────────────────────────────────────

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}
