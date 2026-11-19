package io.github.senagithub.tracker.content;

/**
 * Optional filters for listing content; null / false means "don't filter on this".
 *
 * @param overdue     when true, only content past its due date that is not published
 * @param dueThisWeek when true, only unpublished content due today or in the next 6 days
 */
public record ContentFilter(
        ContentStatus status,
        Long clientId,
        ContentType type,
        String query,
        boolean overdue,
        boolean dueThisWeek
) {
}
