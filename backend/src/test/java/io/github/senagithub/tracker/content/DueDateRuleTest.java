package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.common.FieldValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** The due date rule with a fixed "today", so it doesn't depend on the clock. */
class DueDateRuleTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Test
    void newContentDueTodayIsAccepted() {
        assertDoesNotThrow(() -> DueDateRule.check(TODAY, null, TODAY));
    }

    @Test
    void newContentDueYesterdayIsRejected() {
        FieldValidationException e = assertThrows(FieldValidationException.class,
                () -> DueDateRule.check(TODAY.minusDays(1), null, TODAY));
        assertEquals("dueDate", e.getField());
        assertEquals("Due date cannot be in the past.", e.getMessage());
    }

    @Test
    void editKeepingAnUnchangedPastDateIsAccepted() {
        LocalDate saved = TODAY.minusDays(5);
        assertDoesNotThrow(() -> DueDateRule.check(saved, saved, TODAY));
    }

    @Test
    void editMovingToAnotherPastDateIsRejected() {
        assertThrows(FieldValidationException.class,
                () -> DueDateRule.check(TODAY.minusDays(2), TODAY.minusDays(5), TODAY));
    }

    @Test
    void editMovingToTomorrowIsAccepted() {
        assertDoesNotThrow(() -> DueDateRule.check(TODAY.plusDays(1), TODAY.minusDays(5), TODAY));
    }
}
