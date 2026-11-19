package io.github.senagithub.tracker.client.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientRequest(
        @NotBlank(message = "Name is required.")
        @Size(max = 120, message = "Name must be at most 120 characters.")
        String name,

        @NotBlank(message = "Industry is required.")
        @Size(max = 80, message = "Industry must be at most 80 characters.")
        String industry,

        @NotBlank(message = "Contact name is required.")
        @Size(max = 120, message = "Contact name must be at most 120 characters.")
        String contactName,

        @NotBlank(message = "Contact email is required.")
        @Email(message = "Contact email must be a valid email address.")
        @Size(max = 254, message = "Contact email must be at most 254 characters.")
        String contactEmail
) {
}
