package io.github.senagithub.tracker.activity;

import io.github.senagithub.tracker.activity.ActivityLogRepository.QueueOrder;
import io.github.senagithub.tracker.activity.dto.ActivityResponse;
import io.github.senagithub.tracker.content.Content;
import io.github.senagithub.tracker.content.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ActivityService {

    private final ActivityLogRepository activityLogRepository;

    public ActivityService(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    /** Joins the caller's transaction: if the content change rolls back, so does its log entry. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(Content content, ActivityAction action, ContentStatus from, ContentStatus to) {
        activityLogRepository.save(new ActivityLog(content, action, from, to));
    }

    /** A content currently in some status, and when it last moved into it. */
    public record StatusSince(Long contentId, Instant since) {
    }

    /**
     * Content currently in {@code status}, one page of it, in the given order.
     *
     * @param clientId only this client's content, or every client when null
     * @param page     page index and size; any sort on it is replaced by {@code order}
     */
    @Transactional(readOnly = true)
    public Page<StatusSince> inStatus(ContentStatus status, Long clientId, QueueOrder order, Pageable page) {
        Pageable sorted = PageRequest.of(page.getPageNumber(), page.getPageSize(), order.sort());
        return activityLogRepository.findInStatus(status, clientId, sorted)
                .map(row -> new StatusSince(row.getContentId(), row.getSince()));
    }

    /** How many status changes into {@code status} happened in [from, to). */
    @Transactional(readOnly = true)
    public long countMovedInto(ContentStatus status, Instant from, Instant to) {
        return activityLogRepository.countByToStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(status, from, to);
    }

    /** A content's history, newest first. The caller checks that the content exists. */
    @Transactional(readOnly = true)
    public List<ActivityResponse> history(Long contentId) {
        return activityLogRepository.findByContentIdOrderByCreatedAtDescIdDesc(contentId).stream()
                .map(ActivityResponse::from)
                .toList();
    }
}
