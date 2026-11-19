package io.github.senagithub.tracker.client;

import io.github.senagithub.tracker.common.LikePattern;
import org.springframework.data.jpa.domain.Specification;

final class ClientSpecifications {

    private ClientSpecifications() {
    }

    /** Case-insensitive match on name, industry or contact name; a blank query matches everything. */
    static Specification<Client> matching(String query) {
        if (query == null || query.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = LikePattern.containing(query);
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("industry")), pattern, LikePattern.ESCAPE),
                cb.like(cb.lower(root.get("contactName")), pattern, LikePattern.ESCAPE));
    }
}
