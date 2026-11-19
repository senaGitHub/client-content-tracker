package io.github.senagithub.tracker.activity.dto;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityLog;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.dto.ContentResponse.ClientRef;

import java.time.Instant;

public record ActivityResponse(
        Long id,
        ActivityAction action,
        ContentStatus fromStatus,
        ContentStatus toStatus,
        Instant createdAt,
        ContentRef content,
        ClientRef client
) {

    public record ContentRef(Long id, String title) {
    }

    public static ActivityResponse from(ActivityLog entry) {
        Content content = entry.getContent();
        return new ActivityResponse(
                entry.getId(),
                entry.getAction(),
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getCreatedAt(),
                new ContentRef(content.getId(), content.getTitle()),
                new ClientRef(content.getClient().getId(), content.getClient().getName()));
    }
}
