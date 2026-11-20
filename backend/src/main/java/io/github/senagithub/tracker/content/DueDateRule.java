package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.common.FieldValidationException;

import java.time.LocalDate;

/**
 * A due date can't be set in the past. On create it must be today or later; on edit an unchanged
 * date may stay in the past (an overdue item must stay editable), but a changed one must be today
 * or later. "Today" is the caller's LocalDate.now(), the same day the overdue rule uses.
 * The frontend mirrors this in src/lib/validation.ts (checkDueDate).
 */
public final class DueDateRule {

    static final String MESSAGE = "Due date cannot be in the past.";

    private DueDateRule() {
    }

    /**
     * @param dueDate  the requested due date (Bean Validation already rejects null)
     * @param previous the saved due date when editing, or null when creating
     * @param today    the server's current date
     * @throws FieldValidationException on "dueDate" (400) when the rule is broken
     */
    public static void check(LocalDate dueDate, LocalDate previous, LocalDate today) {
        if (dueDate == null || dueDate.equals(previous)) {
            return;
        }
        if (dueDate.isBefore(today)) {
            throw new FieldValidationException("dueDate", MESSAGE);
        }
    }
}
