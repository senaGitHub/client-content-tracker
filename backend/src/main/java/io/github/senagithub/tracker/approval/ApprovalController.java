package io.github.senagithub.tracker.approval;

import io.github.senagithub.tracker.approval.dto.ApprovalItem;
import io.github.senagithub.tracker.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @Operation(summary = "Content waiting for approval (IN_REVIEW); paged, longest-waiting first by default",
            description = "Each item is a content response plus inReviewSince: when it last moved into "
                    + "IN_REVIEW, from the activity log. sort is waiting,desc (the default: longest-"
                    + "waiting first) or waiting,asc (shortest-waiting first); equal values are ordered by id. "
                    + "Anything else is a 400. Page size defaults "
                    + "to 20 and is capped at 100. Without clientId, totalElements equals the dashboard's "
                    + "totals.inReview.")
    @ApiResponse(responseCode = "400", description = "Unknown sort")
    // page and size come from the Pageable; its own sort binding is hidden, since sort is the whitelist below.
    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Zero-based page index",
            schema = @Schema(type = "integer", defaultValue = "0"))
    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Page size (max 100)",
            schema = @Schema(type = "integer", defaultValue = "20"))
    public PageResponse<ApprovalItem> list(
            @Parameter(description = "Only this client's content")
            @RequestParam(required = false) Long clientId,
            @Parameter(description = "Order of the queue", schema = @Schema(allowableValues = {"waiting,desc", "waiting,asc"},
                    defaultValue = "waiting,desc"))
            @RequestParam(required = false) String sort,
            @Parameter(hidden = true) @PageableDefault(size = 20) Pageable pageable) {
        return approvalService.page(clientId, ApprovalSort.fromParam(sort), pageable);
    }
}
