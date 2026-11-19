package io.github.senagithub.tracker.dashboard.dto;

/**
 * Small real-data facts shown next to the totals. "This week" is the last 7 days (rolling),
 * "last week" the 7 days before that.
 *
 * @param oldestInReviewDays days the longest-waiting review has been waiting; null when nothing is in review
 * @param oldestOverdueDays  days since the oldest overdue due date; null when nothing is overdue
 */
public record DashboardMetrics(
        long createdThisWeek,
        long publishedThisWeek,
        long publishedLastWeek,
        Long oldestInReviewDays,
        Long oldestOverdueDays
) {
}
