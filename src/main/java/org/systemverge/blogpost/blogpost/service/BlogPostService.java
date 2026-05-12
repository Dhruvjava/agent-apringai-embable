package org.systemverge.blogpost.blogpost.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Application service contract for blog post generation and persistence.
 *
 * <p>The service owns the orchestration of the AI agent pipeline and is the
 * single entry-point for both the CLI shell and the REST controller.</p>
 */
public interface BlogPostService {

    /**
     * Runs the full AI agent pipeline (writeDraft → reviewAndSave),
     * persists the result to the database, and returns the saved entity.
     *
     * @param topic the user-supplied topic / prompt
     * @return the persisted {@link BlogPostEntity}
     */
    BlogPostEntity generateAndPersist(String topic);

    /**
     * Returns a paginated list of all blog posts, sorted newest-first.
     */
    Page<BlogPostEntity> findAll(Pageable pageable);

    /**
     * Returns a single blog post by its UUID, or empty if not found.
     */
    Optional<BlogPostEntity> findById(UUID id);

    /**
     * Returns all posts matching the given lifecycle status, newest-first.
     */
    List<BlogPostEntity> findByStatus(BlogPostStatus status);

    /**
     * Case-insensitive keyword search on the original user topic.
     */
    List<BlogPostEntity> search(String keyword);

    /**
     * Permanently deletes a blog post by its UUID.
     *
     * @throws jakarta.persistence.EntityNotFoundException if not found
     */
    void deleteById(UUID id);
}
