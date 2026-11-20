package io.github.senagithub.tracker.dashboard;

import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.approval.ApprovalService;
import io.github.senagithub.tracker.approval.ApprovalSort;
import io.github.senagithub.tracker.approval.dto.ApprovalItem;
import io.github.senagithub.tracker.client.ClientService;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentSpecifications;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.dashboard.dto.DashboardMetrics;
import io.github.senagithub.tracker.dashboard.dto.DashboardResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** How many items the Overview's approvals panel shows; "Tüm onaylar" opens the rest. */
    public static final int APPROVALS_LIMIT = 4;

    private static final Duration WEEK = Duration.ofDays(7);

    private final ClientService clientService;
    private final ContentRepository contentRepository;
    private final ActivityService activityService;
    private final ApprovalService approvalService;

    public DashboardService(ClientService clientService,
                            ContentRepository contentRepository,
                            ActivityService activityService,
                            ApprovalService approvalService) {
        this.clientService = clientService;
        this.contentRepository = contentRepository;
        this.activityService = activityService;
        this.approvalService = approvalService;
    }

    public DashboardResponse get() {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        Instant weekAgo = now.minus(WEEK);
        Instant twoWeeksAgo = weekAgo.minus(WEEK);

        List<ClientResponse> clients = clientService.list();
        // The first page of the same queue as GET /api/approvals, so the panel and the page never disagree.
        List<ApprovalItem> approvals = approvalService.page(null, ApprovalSort.DEFAULT, PageRequest.of(0, APPROVALS_LIMIT)).items();

        DashboardMetrics metrics = new DashboardMetrics(
                contentRepository.count(ContentSpecifications.createdSince(weekAgo)),
                activityService.countMovedInto(ContentStatus.PUBLISHED, weekAgo, now),
                activityService.countMovedInto(ContentStatus.PUBLISHED, twoWeeksAgo, weekAgo),
                approvals.isEmpty() ? null : daysSince(approvals.getFirst().inReviewSince(), today),
                oldestOverdueDays(today));

        return new DashboardResponse(sumCounts(clients), metrics, clients, approvals);
    }

    /** Uses the same overdue specification as the list filter, so the rule stays in one place. */
    private Long oldestOverdueDays(LocalDate today) {
        return contentRepository.findAll(ContentSpecifications.overdue(today), PageRequest.of(0, 1, ContentSpecifications.BY_DUE_DATE))
                .stream()
                .findFirst()
                .map(content -> ChronoUnit.DAYS.between(content.getDueDate(), today))
                .orElse(null);
    }

    private static long daysSince(Instant moment, LocalDate today) {
        return ChronoUnit.DAYS.between(LocalDate.ofInstant(moment, ZoneId.systemDefault()), today);
    }

    /** Totals are the sum of the per-client counts, so the two can never disagree. */
    private static StatusCounts sumCounts(List<ClientResponse> clients) {
        long draft = 0, inReview = 0, published = 0, overdue = 0, total = 0;
        for (ClientResponse client : clients) {
            StatusCounts c = client.counts();
            draft += c.draft();
            inReview += c.inReview();
            published += c.published();
            overdue += c.overdue();
            total += c.total();
        }
        return new StatusCounts(draft, inReview, published, overdue, total);
    }
}
