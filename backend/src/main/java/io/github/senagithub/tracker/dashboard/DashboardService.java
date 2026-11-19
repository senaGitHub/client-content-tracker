package io.github.senagithub.tracker.dashboard;

import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.activity.ActivityService.StatusSince;
import io.github.senagithub.tracker.client.ClientService;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentSpecifications;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import io.github.senagithub.tracker.dashboard.dto.DashboardMetrics;
import io.github.senagithub.tracker.dashboard.dto.DashboardResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** How many items the Overview's approvals panel shows; "Tüm onaylar" opens the rest. */
    public static final int APPROVALS_LIMIT = 5;

    private static final Duration WEEK = Duration.ofDays(7);

    private final ClientService clientService;
    private final ContentRepository contentRepository;
    private final ActivityService activityService;

    public DashboardService(ClientService clientService,
                            ContentRepository contentRepository,
                            ActivityService activityService) {
        this.clientService = clientService;
        this.contentRepository = contentRepository;
        this.activityService = activityService;
    }

    public DashboardResponse get() {
        LocalDate today = LocalDate.now();
        Instant now = Instant.now();
        Instant weekAgo = now.minus(WEEK);
        Instant twoWeeksAgo = weekAgo.minus(WEEK);

        List<ClientResponse> clients = clientService.list();
        List<StatusSince> waiting = activityService.longestIn(ContentStatus.IN_REVIEW, APPROVALS_LIMIT);

        DashboardMetrics metrics = new DashboardMetrics(
                contentRepository.count(ContentSpecifications.createdSince(weekAgo)),
                activityService.countMovedInto(ContentStatus.PUBLISHED, weekAgo, now),
                activityService.countMovedInto(ContentStatus.PUBLISHED, twoWeeksAgo, weekAgo),
                waiting.isEmpty() ? null : daysSince(waiting.getFirst().since(), today),
                oldestOverdueDays(today));

        return new DashboardResponse(sumCounts(clients), metrics, clients, approvals(waiting, today));
    }

    /** Loads the waiting items in one query and keeps the longest-waiting-first order. */
    private List<ContentResponse> approvals(List<StatusSince> waiting, LocalDate today) {
        if (waiting.isEmpty()) {
            return List.of();
        }
        List<Long> ids = waiting.stream().map(StatusSince::contentId).toList();
        Specification<Content> byIds = (root, q, cb) -> root.get("id").in(ids);
        Map<Long, Content> byId = contentRepository.findAll(byIds, ContentSpecifications.BY_DUE_DATE).stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));
        return ids.stream()
                .map(byId::get)
                .map(content -> ContentResponse.from(content, today))
                .toList();
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
