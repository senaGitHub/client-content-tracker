package io.github.senagithub.tracker.content.dto;

import io.github.senagithub.tracker.content.ContentType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The title rule on ContentRequest, checked with plain Bean Validation (no Spring context, no DB). */
class ContentRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private Set<String> titleMessages(String title) {
        ContentRequest request = new ContentRequest(1L, title, ContentType.SOCIAL_POST, null, LocalDate.now(), null);
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals("title"))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @ParameterizedTest(name = "\"{0}\" is accepted")
    @ValueSource(strings = {"Reels 3", "Gün batımı terası", "2026 yaz kampanyası"})
    void acceptsTitlesWithALetter(String title) {
        assertEquals(Set.of(), titleMessages(title));
    }

    @ParameterizedTest(name = "\"{0}\" is rejected")
    @ValueSource(strings = {"2026", "12.05", "!!!"})
    void rejectsTitlesWithoutALetter(String title) {
        assertEquals(Set.of("Title must contain at least one letter."), titleMessages(title));
    }
}
