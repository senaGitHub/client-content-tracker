package io.github.senagithub.tracker.client.dto;

/** Content counts for one client. {@code overdue} overlaps the status counts; {@code total} doesn't include it twice. */
public record StatusCounts(long draft, long inReview, long published, long overdue, long total) {

    public static final StatusCounts EMPTY = new StatusCounts(0, 0, 0, 0, 0);
}
