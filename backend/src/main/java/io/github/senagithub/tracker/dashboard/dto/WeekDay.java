package io.github.senagithub.tracker.dashboard.dto;

import io.github.senagithub.tracker.content.dto.ContentResponse;

import java.time.LocalDate;
import java.util.List;

/** One day of the week view; {@code items} includes published content and may be empty. */
public record WeekDay(LocalDate date, List<ContentResponse> items) {
}
