package io.github.senagithub.tracker.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body for creating a brand. The text rules use \p{L} (any Unicode letter), so Turkish letters
 * count. The frontend mirrors them in src/lib/validation.ts; change both together.
 */
public record ClientRequest(
        @NotBlank(message = "Name is required.")
        @Size(min = 2, message = "Name must be at least 2 characters.")
        @Size(max = 120, message = "Name must be at most 120 characters.")
        @Pattern(regexp = "(?s).*\\p{L}.*", message = "Name must contain at least one letter.")
        String name,

        @NotBlank(message = "Industry is required.")
        @Size(min = 2, message = "Industry must be at least 2 characters.")
        @Size(max = 80, message = "Industry must be at most 80 characters.")
        @Pattern(regexp = "[\\p{L} &,.\\-]+", message = "Industry may only contain letters, spaces and & - , .")
        String industry,

        @NotBlank(message = "Contact name is required.")
        @Size(min = 2, message = "Contact name must be at least 2 characters.")
        @Size(max = 120, message = "Contact name must be at most 120 characters.")
        @Pattern(regexp = "[\\p{L} '.\\-]+", message = "Contact name may only contain letters, spaces and ' - .")
        String contactName,

        @NotBlank(message = "Contact email is required.")
        @Email(message = "Contact email must be a valid email address.")
        @Size(max = 254, message = "Contact email must be at most 254 characters.")
        String contactEmail,

        @Schema(description = "Optional; one of the bundled cover photos, e.g. /images/covers/palm-pool.jpg")
        String coverImageUrl
) {
}
