package io.github.senagithub.tracker.content.dto;

import io.github.senagithub.tracker.content.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body for creating and editing content. Status is deliberately absent: it changes only via PATCH /status. */
public record ContentRequest(
        @NotNull(message = "Client is required.")
        Long clientId,

        @NotBlank(message = "Title is required.")
        @Size(max = 200, message = "Title must be at most 200 characters.")
        String title,

        @NotNull(message = "Type is required.")
        ContentType type,

        @Size(max = 60, message = "Channel must be at most 60 characters.")
        String channel,

        @NotNull(message = "Due date is required.")
        LocalDate dueDate,

        @Size(max = 5000, message = "Notes must be at most 5000 characters.")
        String notes
) {
}
