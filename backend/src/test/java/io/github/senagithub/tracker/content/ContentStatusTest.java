package io.github.senagithub.tracker.content;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Covers all 9 (from, to) pairs, so any change to the pipeline rule shows up here. */
class ContentStatusTest {

    @ParameterizedTest(name = "{0} -> {1} allowed: {2}")
    @CsvSource({
            "DRAFT,     IN_REVIEW, true",
            "IN_REVIEW, PUBLISHED, true",
            "IN_REVIEW, DRAFT,     true",
            "DRAFT,     DRAFT,     false",
            "DRAFT,     PUBLISHED, false",
            "IN_REVIEW, IN_REVIEW, false",
            "PUBLISHED, DRAFT,     false",
            "PUBLISHED, IN_REVIEW, false",
            "PUBLISHED, PUBLISHED, false",
    })
    void canTransitionTo(ContentStatus from, ContentStatus to, boolean expected) {
        assertEquals(expected, from.canTransitionTo(to));
    }
}
