package io.github.senagithub.tracker.content;

import java.util.Arrays;
import java.util.List;

public enum ContentStatus {
    DRAFT,
    IN_REVIEW,
    PUBLISHED;

    /**
     * The status pipeline: DRAFT → IN_REVIEW → PUBLISHED, plus IN_REVIEW → DRAFT for revisions.
     * Every other move (including staying on the same status) is not allowed.
     */
    public boolean canTransitionTo(ContentStatus target) {
        return switch (this) {
            case DRAFT -> target == IN_REVIEW;
            case IN_REVIEW -> target == PUBLISHED || target == DRAFT;
            case PUBLISHED -> false;
        };
    }

    public List<ContentStatus> allowedTransitions() {
        return Arrays.stream(values()).filter(this::canTransitionTo).toList();
    }
}
