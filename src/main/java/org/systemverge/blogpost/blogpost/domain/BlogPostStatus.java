package org.systemverge.blogpost.blogpost.domain;

/**
 * Lifecycle status of a blog post.
 * <ul>
 *   <li>{@link #DRAFT} – AI has produced an initial draft, not yet reviewed.</li>
 *   <li>{@link #REVIEWED} – AI reviewer has polished the post; ready for publication.</li>
 * </ul>
 */
public enum BlogPostStatus {
    DRAFT,
    REVIEWED
}
