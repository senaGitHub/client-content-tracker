package io.github.senagithub.tracker.content;

/** The requested status change breaks the pipeline rule → 409. */
public class InvalidStatusTransitionException extends RuntimeException {

    private final ContentStatus current;
    private final ContentStatus requested;

    public InvalidStatusTransitionException(ContentStatus current, ContentStatus requested) {
        super(buildMessage(current, requested));
        this.current = current;
        this.requested = requested;
    }

    private static String buildMessage(ContentStatus current, ContentStatus requested) {
        if (current == requested) {
            return "Content is already " + label(current) + ".";
        }
        if (current == ContentStatus.PUBLISHED) {
            return "Published content can't be moved back.";
        }
        return "Can't move content from " + label(current) + " to " + label(requested) + ".";
    }

    private static String label(ContentStatus status) {
        return switch (status) {
            case DRAFT -> "Draft";
            case IN_REVIEW -> "In review";
            case PUBLISHED -> "Published";
        };
    }

    public ContentStatus getCurrent() {
        return current;
    }

    public ContentStatus getRequested() {
        return requested;
    }
}
