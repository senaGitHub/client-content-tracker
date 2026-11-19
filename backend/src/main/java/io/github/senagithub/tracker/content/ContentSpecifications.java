package io.github.senagithub.tracker.content;

import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause for {@code GET /api/contents} from whichever filters were given.
 * Only non-empty filters become conditions, so there are no "(:param is null or ...)" tricks,
 * which Postgres can't type-check for null parameters.
 */
final class ContentSpecifications {

    private ContentSpecifications() {
    }

    static Specification<Content> matching(ContentStatus status, Long clientId, ContentType type, String query) {
        List<Specification<Content>> specs = new ArrayList<>();
        if (status != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (clientId != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("client").get("id"), clientId));
        }
        if (type != null) {
            specs.add((root, q, cb) -> cb.equal(root.get("type"), type));
        }
        if (query != null && !query.isBlank()) {
            String pattern = "%" + escapeLike(query.trim().toLowerCase(Locale.ROOT)) + "%";
            specs.add((root, q, cb) -> cb.like(cb.lower(root.get("title")), pattern, '\\'));
        }
        return Specification.allOf(specs);
    }

    /** So a search for "50%" matches the text "50%" instead of treating % as a wildcard. */
    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
