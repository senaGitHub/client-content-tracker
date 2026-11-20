package io.github.senagithub.tracker.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.github.senagithub.tracker.content.dto.ContentResponse;

import java.time.Instant;

/**
 * An item in the Overview's approvals queue: the usual content fields (unwrapped, so the JSON
 * stays flat and matches a plain content response) plus when it entered review.
 *
 * @param inReviewSince when the content last moved into IN_REVIEW, from the activity log
 */
public record ApprovalItem(
        @JsonUnwrapped ContentResponse content,
        Instant inReviewSince
) {
}
