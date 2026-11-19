package io.github.senagithub.tracker.dashboard;

import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.client.ClientService;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import io.github.senagithub.tracker.dashboard.dto.DashboardResponse;
import io.github.senagithub.tracker.dashboard.dto.NeedsAttention;
import io.github.senagithub.tracker.dashboard.dto.WeekDay;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** "This week" is a rolling window: today plus the next 6 days. */
    private static final int WEEK_DAYS = 7;

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
        LocalDate lastDay = today.plusDays(WEEK_DAYS - 1);

        List<ClientResponse> clients = clientService.list();

        List<ContentResponse> weekItems = toResponses(
                contentRepository.findByDueDateBetweenOrderByDueDateAscIdAsc(today, lastDay), today);

        // Due this week = the week window minus what's already published; no extra query needed.
        List<ContentResponse> dueThisWeek = weekItems.stream()
                .filter(item -> item.status() != ContentStatus.PUBLISHED)
                .toList();

        NeedsAttention needsAttention = new NeedsAttention(
                toResponses(contentRepository.findByDueDateBeforeAndStatusNotOrderByDueDateAscIdAsc(
                        today, ContentStatus.PUBLISHED), today),
                dueThisWeek,
                toResponses(contentRepository.findByStatusOrderByDueDateAscIdAsc(ContentStatus.IN_REVIEW), today));

        return new DashboardResponse(
                sumCounts(clients),
                needsAttention,
                groupByDay(weekItems, today),
                clients,
                activityService.recent());
    }

    /** Always 7 entries, including days with nothing due, so the UI can render a stable strip. */
    private static List<WeekDay> groupByDay(List<ContentResponse> items, LocalDate today) {
        Map<LocalDate, List<ContentResponse>> byDate = items.stream()
                .collect(Collectors.groupingBy(ContentResponse::dueDate));
        return IntStream.range(0, WEEK_DAYS)
                .mapToObj(today::plusDays)
                .map(date -> new WeekDay(date, byDate.getOrDefault(date, List.of())))
                .toList();
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

    private static List<ContentResponse> toResponses(List<Content> contents, LocalDate today) {
        return contents.stream().map(content -> ContentResponse.from(content, today)).toList();
    }
}
