package io.github.senagithub.tracker.activity;

import io.github.senagithub.tracker.content.ContentStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    /** Newest first; id breaks ties between entries written in the same instant. */
    List<ActivityLog> findByContentIdOrderByCreatedAtDescIdDesc(Long contentId);

    /** Transitions into {@code status} in [from, to). Publishing can't be undone, so each content counts once. */
    long countByToStatusAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(ContentStatus status, Instant from, Instant to);

    /** When a content entered its current status; one row per content. */
    interface StatusSinceRow {
        Long getContentId();

        Instant getSince();
    }

    /**
     * Content currently in {@code status}, with the moment it last moved into it, longest-waiting first.
     * "Last" matters for content that went back to draft and was sent for review again.
     */
    @Query("""
            select a.content.id as contentId, max(a.createdAt) as since
            from ActivityLog a
            where a.toStatus = :status and a.content.status = :status
            group by a.content.id
            order by max(a.createdAt) asc, a.content.id asc
            """)
    List<StatusSinceRow> findLongestInStatus(ContentStatus status, Limit limit);
}
