package io.github.senagithub.tracker.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;
import java.util.TreeSet;

/** Turns the client-supplied page/size/sort into a safe, deterministic {@link Pageable}. */
public final class Paging {

    private Paging() {
    }

    /**
     * Applies the default sort when none is given, rejects unknown sort properties with a 400
     * (instead of a 500 from Hibernate), and appends {@code id} as a tie-breaker so rows with
     * equal sort values never jump between pages.
     */
    public static Pageable sanitize(Pageable pageable, Sort defaultSort, Set<String> sortable) {
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : defaultSort;
        for (Sort.Order order : sort) {
            if (!sortable.contains(order.getProperty()) && !order.getProperty().equals("id")) {
                throw new FieldValidationException("sort", "Cannot sort by '" + order.getProperty()
                        + "'. Allowed: " + String.join(", ", new TreeSet<>(sortable)) + ".");
            }
        }
        if (sort.getOrderFor("id") == null) {
            sort = sort.and(Sort.by("id"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }
}
