package io.github.senagithub.tracker.activity.dto;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityLog;
import io.github.senagithub.tracker.content.ContentStatus;

import java.time.Instant;

/** One entry in a content's history (the edit sheet's "Geçmiş"). */
public record ActivityResponse(
        Long id,
        ActivityAction action,
        ContentStatus fromStatus,
        ContentStatus toStatus,
        Instant createdAt
) {

    public static ActivityResponse from(ActivityLog entry) {
        return new ActivityResponse(
                entry.getId(),
                entry.getAction(),
                entry.getFromStatus(),
                entry.getToStatus(),
                entry.getCreatedAt());
    }
}
