package io.github.senagithub.tracker.common;

/** A requested resource (by id in the URL) does not exist → 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String resource, Long id) {
        super(resource + " " + id + " was not found.");
    }
}
