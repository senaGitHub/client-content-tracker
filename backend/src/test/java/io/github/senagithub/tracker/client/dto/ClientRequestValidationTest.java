package io.github.senagithub.tracker.client.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The text rules on ClientRequest, checked with plain Bean Validation (no Spring context, no DB). */
class ClientRequestValidationTest {

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

    private static ClientRequest request(String name, String industry, String contactName) {
        return new ClientRequest(name, industry, contactName, "ayse@example.com", null);
    }

    /** Messages for one field only, so a test is not affected by the other fields. */
    private Set<String> messagesFor(ClientRequest request, String field) {
        return validator.validate(request).stream()
                .filter(v -> v.getPropertyPath().toString().equals(field))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void acceptsAValidBrand() {
        assertEquals(Set.of(), validator.validate(request("Aegean Blue Hotel 2", "Otel & Spa, Kafe.", "Ayşe O'Neil-Kaya")));
    }

    @Test
    void acceptsTurkishLetters() {
        assertEquals(Set.of(), validator.validate(request("Çınar Şile", "Sağlık turizmi", "İlkay Güneş")));
    }

    @ParameterizedTest(name = "{0} = \"{1}\" -> {2}")
    @CsvSource(delimiter = '|', value = {
            "name        | 123     | Name must contain at least one letter.",
            "name        | 7       | Name must be at least 2 characters.",
            "industry    | Otel 5* | Industry may only contain letters, spaces and & - , .",
            "industry    | A       | Industry must be at least 2 characters.",
            "contactName | Ali2    | Contact name may only contain letters, spaces and ' - .",
            "contactName | A       | Contact name must be at least 2 characters.",
    })
    void rejectsBrokenText(String field, String value, String expectedMessage) {
        ClientRequest request = switch (field) {
            case "name" -> request(value, "Butik otel", "Ayşe Kaya");
            case "industry" -> request("Aegean Blue", value, "Ayşe Kaya");
            case "contactName" -> request("Aegean Blue", "Butik otel", value);
            default -> throw new IllegalArgumentException(field);
        };
        assertTrue(messagesFor(request, field).contains(expectedMessage),
                () -> field + " messages: " + messagesFor(request, field));
    }
}
