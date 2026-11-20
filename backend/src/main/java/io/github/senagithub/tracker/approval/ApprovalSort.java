package io.github.senagithub.tracker.approval;

import io.github.senagithub.tracker.activity.ActivityLogRepository.QueueOrder;
import io.github.senagithub.tracker.common.FieldValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * The sort options of GET /api/approvals, written like the other lists' {@code property,direction}
 * (a whitelist: anything else is a 400). "waiting" is how long an item has been in review, so
 * {@code waiting,desc} puts the longest-waiting first. Equal values are always broken by the
 * content id, so pages never overlap.
 */
public enum ApprovalSort {

    /** Longest-waiting first (the default, and the Overview panel's order). */
    WAITING_DESC("waiting,desc", QueueOrder.LONGEST_WAITING),
    /** Shortest-waiting (most recently sent) first. */
    WAITING_ASC("waiting,asc", QueueOrder.SHORTEST_WAITING);

    public static final ApprovalSort DEFAULT = WAITING_DESC;

    private final String param;
    private final QueueOrder order;

    ApprovalSort(String param, QueueOrder order) {
        this.param = param;
        this.order = order;
    }

    public String param() {
        return param;
    }

    QueueOrder order() {
        return order;
    }

    /** Parses the query parameter; null or blank means the default. */
    public static ApprovalSort fromParam(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(sort -> sort.param.equals(trimmed))
                .findFirst()
                .orElseThrow(() -> new FieldValidationException("sort", "Cannot sort approvals by '" + value
                        + "'. Allowed: " + Arrays.stream(values()).map(ApprovalSort::param).collect(Collectors.joining(", "))
                        + "."));
    }
}
