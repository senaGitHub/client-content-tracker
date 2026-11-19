package io.github.senagithub.tracker.content;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long>, JpaSpecificationExecutor<Content> {

    /** Loads the client in the same query, so mapping a list to DTOs doesn't fire one query per row. */
    @Override
    @EntityGraph(attributePaths = "client")
    List<Content> findAll(@Nullable Specification<Content> spec, Sort sort);

    // --- Dashboard lists (client loaded in the same query) ---

    /** Every content due in [from, to], whatever its status — the dashboard's week view. */
    @EntityGraph(attributePaths = "client")
    List<Content> findByDueDateBetweenOrderByDueDateAscIdAsc(LocalDate from, LocalDate to);

    /** Used with {@code status = PUBLISHED} to find overdue content. */
    @EntityGraph(attributePaths = "client")
    List<Content> findByDueDateBeforeAndStatusNotOrderByDueDateAscIdAsc(LocalDate date, ContentStatus status);

    @EntityGraph(attributePaths = "client")
    List<Content> findByStatusOrderByDueDateAscIdAsc(ContentStatus status);

    // --- Per-client counts ---

    /** One row per (client, status) pair that has content. */
    interface StatusCountRow {
        Long getClientId();

        ContentStatus getStatus();

        long getTotal();
    }

    interface OverdueCountRow {
        Long getClientId();

        long getTotal();
    }

    @Query("""
            select c.client.id as clientId, c.status as status, count(c) as total
            from Content c
            group by c.client.id, c.status
            """)
    List<StatusCountRow> countByClientAndStatus();

    @Query("""
            select c.client.id as clientId, c.status as status, count(c) as total
            from Content c
            where c.client.id = :clientId
            group by c.client.id, c.status
            """)
    List<StatusCountRow> countByStatusForClient(Long clientId);

    @Query("""
            select c.client.id as clientId, count(c) as total
            from Content c
            where c.dueDate < :today
              and c.status <> io.github.senagithub.tracker.content.ContentStatus.PUBLISHED
            group by c.client.id
            """)
    List<OverdueCountRow> countOverdueByClient(LocalDate today);
}
