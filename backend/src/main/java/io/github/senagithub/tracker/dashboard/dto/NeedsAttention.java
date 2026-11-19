package io.github.senagithub.tracker.dashboard.dto;

import io.github.senagithub.tracker.content.dto.ContentResponse;

import java.util.List;

/**
 * Three independent questions, so one content can appear in more than one list
 * (e.g. overdue and awaiting approval). Published content never appears here.
 */
public record NeedsAttention(
        List<ContentResponse> overdue,
        List<ContentResponse> dueThisWeek,
        List<ContentResponse> awaitingApproval
) {
}
