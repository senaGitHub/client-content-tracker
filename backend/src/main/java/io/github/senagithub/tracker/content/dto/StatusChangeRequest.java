package io.github.senagithub.tracker.content.dto;

import io.github.senagithub.tracker.content.ContentStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull(message = "Status is required.")
        ContentStatus status
) {
}
