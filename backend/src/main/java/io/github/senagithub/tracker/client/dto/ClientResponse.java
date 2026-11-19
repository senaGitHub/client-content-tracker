package io.github.senagithub.tracker.client.dto;

import io.github.senagithub.tracker.client.Client;

import java.time.Instant;

public record ClientResponse(
        Long id,
        String name,
        String industry,
        String contactName,
        String contactEmail,
        String coverImageUrl,
        Instant createdAt,
        StatusCounts counts
) {

    public static ClientResponse from(Client client, StatusCounts counts) {
        return new ClientResponse(
                client.getId(),
                client.getName(),
                client.getIndustry(),
                client.getContactName(),
                client.getContactEmail(),
                client.getCoverImageUrl(),
                client.getCreatedAt(),
                counts);
    }
}
