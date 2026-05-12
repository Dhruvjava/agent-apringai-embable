package org.systemverge.blogpost.blogpost.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for the blog-post generation endpoint.
 *
 * <p>Declared as a record to be immutable by design; Jackson deserialises it
 * via the compact constructor automatically in Spring Boot 4.</p>
 */
public record GenerateBlogPostRequest(

        @NotBlank(message = "Topic must not be blank")
        @Size(min = 3, max = 500, message = "Topic must be between 3 and 500 characters")
        String topic
) {}
