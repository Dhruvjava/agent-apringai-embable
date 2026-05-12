package org.systemverge.blogpost.blogpost.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link BlogPostEntity}.
 *
 * <p>All query methods follow Spring Data naming conventions to keep the
 * implementation zero-boilerplate and swappable (H2 → PostgreSQL → any JPA-compatible DB).</p>
 */
@Repository
public interface BlogPostRepository extends JpaRepository<BlogPostEntity, UUID> {

    /**
     * Paginated list of all posts, newest first.
     */
    Page<BlogPostEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Filter posts by lifecycle status, newest first.
     */
    List<BlogPostEntity> findByStatusOrderByCreatedAtDesc(BlogPostStatus status);

    /**
     * Case-insensitive keyword search on the original user topic.
     */
    List<BlogPostEntity> findByTopicContainingIgnoreCaseOrderByCreatedAtDesc(String keyword);
}
