package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.activity.dto.ActivityResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.common.PageResponse;
import io.github.senagithub.tracker.content.dto.ContentRequest;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import io.github.senagithub.tracker.content.dto.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contents")
@Tag(name = "Contents")
public class ContentController {

    private final ContentService contentService;

    public ContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    @GetMapping
    @Operation(summary = "List contents, optionally filtered; paged, sorted by due date unless ?sort= is given",
            description = "Sortable by dueDate, title, status, type, createdAt, updatedAt, client.name. "
                    + "Page size defaults to 20 and is capped at 100.")
    public PageResponse<ContentResponse> list(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) ContentType type,
            @Parameter(description = "Case-insensitive search in the title")
            @RequestParam(name = "q", required = false) String query,
            @Parameter(description = "Only past-due content that is not published")
            @RequestParam(defaultValue = "false") boolean overdue,
            @Parameter(description = "Only unpublished content due today or in the next 6 days")
            @RequestParam(defaultValue = "false") boolean dueThisWeek,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return contentService.list(new ContentFilter(status, clientId, type, query, overdue, dueThisWeek), pageable);
    }

    @GetMapping("/status-counts")
    @Operation(summary = "Counts per status (and overdue) for the same filters as the list, without status",
            description = "Feeds the list's status tabs, e.g. 'Taslak (52)'.")
    public StatusCounts statusCounts(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) ContentType type,
            @Parameter(description = "Case-insensitive search in the title")
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(defaultValue = "false") boolean overdue,
            @RequestParam(defaultValue = "false") boolean dueThisWeek) {
        return contentService.statusCounts(new ContentFilter(null, clientId, type, query, overdue, dueThisWeek));
    }

    @GetMapping("/{id}/activity")
    @Operation(summary = "History of one content (created, edited, status changes), newest first")
    @ApiResponse(responseCode = "404", description = "Content not found")
    public List<ActivityResponse> activity(@PathVariable Long id) {
        return contentService.history(id);
    }

    @PostMapping
    @Operation(summary = "Create content (always starts as DRAFT)")
    public ResponseEntity<ContentResponse> create(@Valid @RequestBody ContentRequest request) {
        ContentResponse created = contentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit content fields (status is changed via PATCH /status)")
    public ContentResponse update(@PathVariable Long id, @Valid @RequestBody ContentRequest request) {
        return contentService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Move content through the pipeline: DRAFT → IN_REVIEW → PUBLISHED, IN_REVIEW → DRAFT")
    @ApiResponse(responseCode = "200", description = "Status changed")
    @ApiResponse(responseCode = "409", description = "Transition not allowed")
    public ContentResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return contentService.changeStatus(id, request.status());
    }
}
