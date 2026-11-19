package io.github.senagithub.tracker.common;

/**
 * A request field is well-formed but invalid in a way only the service can tell
 * (e.g. it references a client that does not exist) → 400 with a field error,
 * the same shape as Bean Validation errors so forms can show it next to the field.
 */
public class FieldValidationException extends RuntimeException {

    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
