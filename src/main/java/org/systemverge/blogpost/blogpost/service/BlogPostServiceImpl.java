package org.systemverge.blogpost.blogpost.service;

import com.embabel.agent.api.invocation.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import com.embabel.agent.domain.io.UserInput;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.systemverge.blogpost.blogpost.agent.BlogPostAgent;
import org.systemverge.blogpost.blogpost.domain.BlogPostEntity;
import org.systemverge.blogpost.blogpost.domain.BlogPostStatus;
import org.systemverge.blogpost.blogpost.repository.BlogPostRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of {@link BlogPostService}.
 *
 * <p>Orchestration flow:</p>
 * <ol>
 *   <li>Invoke the Embabel {@link BlogPostAgent} pipeline via {@link AgentPlatform}.</li>
 *   <li>Map the agent result to a {@link BlogPostEntity}.</li>
 *   <li>Persist via {@link BlogPostRepository}.</li>
 * </ol>
 *
 * <p>File-saving behaviour of the agent (writing {@code .md} files) is preserved as-is;
 * the database record is an additional, independent persistence target.</p>
 */
@Service
@Transactional(readOnly = true)
public class BlogPostServiceImpl implements BlogPostService {

    private static final Logger log = LoggerFactory.getLogger(BlogPostServiceImpl.class);

    private final AgentPlatform agentPlatform;
    private final BlogPostRepository repository;

    public BlogPostServiceImpl(AgentPlatform agentPlatform, BlogPostRepository repository) {
        this.agentPlatform = agentPlatform;
        this.repository    = repository;
    }

    // ── Write operations ──────────────────────────────────────────────────

    @Override
    @Transactional
    public BlogPostEntity generateAndPersist(String topic) {
        log.info("Starting blog post generation for topic: '{}'", topic);

        BlogPostAgent.ReviewedBlogPost reviewed = invokeAgent(topic);

        BlogPostEntity entity = BlogPostEntity.builder()
                .topic(topic)
                .title(reviewed.title())
                .content(reviewed.content())
                .feedback(reviewed.feedback())
                .status(BlogPostStatus.REVIEWED)
                .build();

        BlogPostEntity saved = repository.save(entity);
        log.info("Blog post persisted to database with id={}, title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    /**
     * Calls the Embabel agent pipeline. Extracted as a protected method
     * to allow subclass overriding in unit tests (Seam pattern).
     */
    protected BlogPostAgent.ReviewedBlogPost invokeAgent(String topic) {
        return AgentInvocation
                .create(agentPlatform, BlogPostAgent.ReviewedBlogPost.class)
                .invoke(new UserInput(topic));
    }


    @Override
    @Transactional
    public void deleteById(UUID id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("Blog post not found with id: " + id);
        }
        repository.deleteById(id);
        log.info("Blog post deleted: id={}", id);
    }

    // ── Read operations ───────────────────────────────────────────────────

    @Override
    public Page<BlogPostEntity> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Override
    public Optional<BlogPostEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<BlogPostEntity> findByStatus(BlogPostStatus status) {
        return repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public List<BlogPostEntity> search(String keyword) {
        return repository.findByTopicContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }
}
