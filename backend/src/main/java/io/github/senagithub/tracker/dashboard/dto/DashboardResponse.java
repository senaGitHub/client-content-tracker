package io.github.senagithub.tracker.dashboard.dto;

import io.github.senagithub.tracker.activity.dto.ActivityResponse;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;

import java.util.List;

public record DashboardResponse(
        StatusCounts totals,
        NeedsAttention needsAttention,
        List<WeekDay> week,
        List<ClientResponse> clients,
        List<ActivityResponse> recentActivity
) {
}
