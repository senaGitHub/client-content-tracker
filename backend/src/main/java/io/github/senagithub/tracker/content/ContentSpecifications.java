package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.common.LikePattern;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Query building blocks for content. The "overdue" and "due this week" definitions live here
 * once and are shared by {@code GET /api/contents} filters and the dashboard, so the list a
 * dashboard link opens always matches the number the dashboard showed.
 * <p>
 * Only non-empty filters become conditions, so there are no "(:param is null or ...)" tricks,
 * which Postgres can't type-check for null parameters.
 */
public final class ContentSpecifications {

    /** "This week" is a rolling window: today plus the next 6 days. */
    public static final int WEEK_DAYS = 7;

    /** Soonest due first; id keeps the order stable when dates are equal. */
    public static final Sort BY_DUE_DATE = Sort.by("dueDate", "id");

    private ContentSpecifications() {
    }

    public static LocalDate lastDayOfWeek(LocalDate today) {
        return today.plusDays(WEEK_DAYS - 1);
    }

    public static Specification<Content> hasStatus(ContentStatus status) {
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    /** Same rule as {@link Content#isOverdue(LocalDate)}: past due and not published. */
    public static Specification<Content> overdue(LocalDate today) {
        Specification<Content> pastDue = (root, q, cb) -> cb.lessThan(root.get("dueDate"), today);
        return pastDue.and(notPublished());
    }

    /** Created at or after {@code from} (the dashboard's "+N bu hafta"). */
    public static Specification<Content> createdSince(Instant from) {
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from);
    }

    /** Everything due in the week window, whatever its status. */
    private static Specification<Content> dueInWeek(LocalDate today) {
        LocalDate lastDay = lastDayOfWeek(today);
        return (root, q, cb) -> cb.between(root.get("dueDate"), today, lastDay);
    }

    /** Due in the week window and still needing work. */
    public static Specification<Content> dueThisWeek(LocalDate today) {
        return dueInWeek(today).and(notPublished());
    }

    static Specification<Content> matching(ContentFilter filter, LocalDate today) {
        List<Specification<Content>> specs = new ArrayList<>();
        if (filter.status() != null) {
            specs.add(hasStatus(filter.status()));
        }
        if (filter.clientId() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("client").get("id"), filter.clientId()));
        }
        if (filter.type() != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("type"), filter.type()));
        }
        if (filter.query() != null && !filter.query().isBlank()) {
            String pattern = LikePattern.containing(filter.query());
            specs.add((root, q, cb) -> cb.like(cb.lower(root.get("title")), pattern, LikePattern.ESCAPE));
        }
        if (filter.overdue()) {
            specs.add(overdue(today));
        }
        if (filter.dueThisWeek()) {
            specs.add(dueThisWeek(today));
        }
        return Specification.allOf(specs);
    }

    /**
     * Orders by what needs attention (the {@code ?sort=priority} key): unpublished work first by
     * due date, so overdue items lead and the nearest deadlines follow; published items last,
     * newest first. Adds no condition.
     * <p>
     * A CASE expression can't be written as a Spring Data {@link Sort}, so this sets the ORDER BY
     * itself; the caller must pass an unsorted Pageable, or that sort would replace this one.
     */
    public static Specification<Content> byPriority() {
        return (root, query, cb) -> {
            // Spring runs the same specification for the page's count query, which must not be ordered.
            if (query != null && !isCountQuery(query)) {
                Predicate published = cb.equal(root.get("status"), ContentStatus.PUBLISHED);
                Path<LocalDate> due = root.get("dueDate");
                Expression<LocalDate> none = cb.nullLiteral(LocalDate.class);
                query.orderBy(
                        cb.asc(cb.<Integer>selectCase().when(published, 1).otherwise(0)),
                        cb.asc(cb.<LocalDate>selectCase().when(published, none).otherwise(due)),
                        cb.desc(cb.<LocalDate>selectCase().when(published, due).otherwise(none)),
                        cb.asc(root.get("id")));
            }
            return cb.conjunction();
        };
    }

    private static boolean isCountQuery(CriteriaQuery<?> query) {
        Class<?> type = query.getResultType();
        return Long.class.equals(type) || long.class.equals(type);
    }

    private static Specification<Content> notPublished() {
        return (root, q, cb) -> cb.notEqual(root.get("status"), ContentStatus.PUBLISHED);
    }
}
