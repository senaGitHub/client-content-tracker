package io.github.senagithub.tracker.approval;

import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.activity.ActivityService.StatusSince;
import io.github.senagithub.tracker.approval.dto.ApprovalItem;
import io.github.senagithub.tracker.common.PageResponse;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentStatus;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The approvals queue: content in review, longest-waiting first unless another order is asked for. The one source for both the
 * Overview's approvals panel (via the dashboard) and the Onay Süreci page.
 */
@Service
@Transactional(readOnly = true)
public class ApprovalService {

    private final ActivityService activityService;
    private final ContentRepository contentRepository;

    public ApprovalService(ActivityService activityService, ContentRepository contentRepository) {
        this.activityService = activityService;
        this.contentRepository = contentRepository;
    }

    /**
     * One page of the queue.
     *
     * @param clientId only this client's content, or every client when null
     * @param sort     the order (whitelisted; ties broken by content id)
     * @param pageable page index and size; its own sort, if any, is ignored
     */
    public PageResponse<ApprovalItem> page(Long clientId, ApprovalSort sort, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Page<StatusSince> waiting = activityService.inStatus(ContentStatus.IN_REVIEW, clientId, sort.order(), pageable);
        Map<Long, Content> byId = loadContents(waiting.getContent());
        // The activity log's "since" travels with each item, so the UI can say how long it has waited.
        return PageResponse.from(waiting,
                entry -> new ApprovalItem(ContentResponse.from(byId.get(entry.contentId()), today), entry.since()));
    }

    /** Loads the page's contents (with their clients) in one query; the queue's order comes from the log. */
    private Map<Long, Content> loadContents(List<StatusSince> waiting) {
        if (waiting.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = waiting.stream().map(StatusSince::contentId).toList();
        Specification<Content> byIds = (root, query, cb) -> root.get("id").in(ids);
        return contentRepository.findAll(byIds).stream()
                .collect(Collectors.toMap(Content::getId, Function.identity()));
    }
}
