package org.systemverge.blogpost.blogpost.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a persisted blog post.
 *
 * <p>Schema is PostgreSQL-compatible; for local development H2 is used
 * via {@code MODE=PostgreSQL} in the JDBC URL.</p>
 *
 * <p>Lifecycle managed by {@link org.systemverge.blogpost.blogpost.service.BlogPostServiceImpl}.</p>
 */
@Entity
@Table(name = "blog_posts")
public class BlogPostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The original topic / prompt submitted by the user.
     */
    @Column(name = "topic", nullable = false, length = 500)
    private String topic;

    /**
     * SEO-friendly title produced by the AI reviewer.
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Full blog post content in Markdown format.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Editor's feedback from the AI reviewer step.
     */
    @Column(name = "feedback", columnDefinition = "TEXT")
    private String feedback;

    /**
     * Lifecycle status of the post.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BlogPostStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── Constructors ──────────────────────────────────────────────────────

    protected BlogPostEntity() {
        // Required by JPA
    }

    private BlogPostEntity(Builder builder) {
        this.topic    = builder.topic;
        this.title    = builder.title;
        this.content  = builder.content;
        this.feedback = builder.feedback;
        this.status   = builder.status;
    }

    // ── Builder ───────────────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String topic;
        private String title;
        private String content;
        private String feedback;
        private BlogPostStatus status = BlogPostStatus.REVIEWED;

        private Builder() {}

        public Builder topic(String topic)         { this.topic    = topic;    return this; }
        public Builder title(String title)         { this.title    = title;    return this; }
        public Builder content(String content)     { this.content  = content;  return this; }
        public Builder feedback(String feedback)   { this.feedback = feedback; return this; }
        public Builder status(BlogPostStatus status){ this.status  = status;   return this; }

        public BlogPostEntity build() {
            if (topic   == null || topic.isBlank())   throw new IllegalStateException("topic is required");
            if (title   == null || title.isBlank())   throw new IllegalStateException("title is required");
            if (content == null || content.isBlank()) throw new IllegalStateException("content is required");
            return new BlogPostEntity(this);
        }
    }

    // ── Getters (no setters — entity state managed via service layer) ─────

    public UUID getId()             { return id; }
    public String getTopic()        { return topic; }
    public String getTitle()        { return title; }
    public String getContent()      { return content; }
    public String getFeedback()     { return feedback; }
    public BlogPostStatus getStatus(){ return status; }
    public Instant getCreatedAt()   { return createdAt; }
    public Instant getUpdatedAt()   { return updatedAt; }

    @Override
    public String toString() {
        return "BlogPostEntity{id=" + id + ", title='" + title + "', status=" + status + "}";
    }
}
