package io.github.senagithub.tracker.content;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /** Everything due in the week window, whatever its status (the dashboard's week strip). */
    public static Specification<Content> dueInWeek(LocalDate today) {
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
            String pattern = "%" + escapeLike(filter.query().trim().toLowerCase(Locale.ROOT)) + "%";
            specs.add((root, q, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        if (filter.overdue()) {
            specs.add(overdue(today));
        }
        if (filter.dueThisWeek()) {
            specs.add(dueThisWeek(today));
        }
        return Specification.allOf(specs);
    }

    private static Specification<Content> notPublished() {
        return (root, q, cb) -> cb.notEqual(root.get("status"), ContentStatus.PUBLISHED);
    }

    /** So a search for "50%" matches the text "50%" instead of treating % as a wildcard. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
