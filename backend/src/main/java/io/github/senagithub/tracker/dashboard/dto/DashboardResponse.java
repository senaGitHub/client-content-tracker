package io.github.senagithub.tracker.dashboard.dto;

import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;

import java.util.List;

/**
 * Everything the Overview page needs in one request.
 *
 * @param approvals the {@value io.github.senagithub.tracker.dashboard.DashboardService#APPROVALS_LIMIT}
 *                  longest-waiting items in review, longest first, each with when it entered review;
 *                  the full count is {@code totals.inReview}
 */
public record DashboardResponse(
        StatusCounts totals,
        DashboardMetrics metrics,
        List<ClientResponse> clients,
        List<ApprovalItem> approvals
) {
}
