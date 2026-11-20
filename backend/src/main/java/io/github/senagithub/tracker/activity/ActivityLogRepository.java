package io.github.senagithub.tracker.activity;

import io.github.senagithub.tracker.content.ContentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.JpaSort;
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
     * Orders for {@link #findInStatus}. They sort on that query's own aggregate, so they are written
     * against its alias {@code a} and live here, next to it.
     * Every order ends with the content id, so equal values never swap places between pages.
     */
    enum QueueOrder {
        /** When it entered the status, oldest first: the longest-waiting item leads. */
        LONGEST_WAITING(JpaSort.unsafe(Sort.Direction.ASC, "max(a.createdAt)")),
        /** When it entered the status, newest first: the shortest-waiting item leads. */
        SHORTEST_WAITING(JpaSort.unsafe(Sort.Direction.DESC, "max(a.createdAt)"));

        private final Sort sort;

        QueueOrder(JpaSort sort) {
            this.sort = sort.andUnsafe(Sort.Direction.ASC, "a.content.id");
        }

        public Sort sort() {
            return sort;
        }
    }

    /**
     * Content currently in {@code status} (optionally of one client), with the moment it last moved
     * into it. "Last" matters for content that went back to draft and was sent for review again.
     * Sort with a {@link QueueOrder} only: plain property sorts don't fit this grouped query.
     */
    @Query(value = """
            select a.content.id as contentId, max(a.createdAt) as since
            from ActivityLog a
            where a.toStatus = :status and a.content.status = :status
              and (:clientId is null or a.content.client.id = :clientId)
            group by a.content.id
            """,
            countQuery = """
            select count(distinct a.content.id)
            from ActivityLog a
            where a.toStatus = :status and a.content.status = :status
              and (:clientId is null or a.content.client.id = :clientId)
            """)
    Page<StatusSinceRow> findInStatus(ContentStatus status, Long clientId, Pageable pageable);
}
