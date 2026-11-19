package io.github.senagithub.tracker.content.dto;

import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.ContentType;

import java.time.Instant;
import java.time.LocalDate;

public record ContentResponse(
        Long id,
        String title,
        ContentType type,
        String channel,
        ContentStatus status,
        LocalDate dueDate,
        String notes,
        ClientRef client,
        /* Computed on the server so the overdue rule exists in one place only. */
        boolean overdue,
        Instant createdAt,
        Instant updatedAt
) {

    public record ClientRef(Long id, String name) {
    }

    public static ContentResponse from(Content content, LocalDate today) {
        return new ContentResponse(
                content.getId(),
                content.getTitle(),
                content.getType(),
                content.getChannel(),
                content.getStatus(),
                content.getDueDate(),
                content.getNotes(),
                new ClientRef(content.getClient().getId(), content.getClient().getName()),
                content.isOverdue(today),
                content.getCreatedAt(),
                content.getUpdatedAt());
    }
}
