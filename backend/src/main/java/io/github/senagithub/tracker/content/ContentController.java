package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.content.dto.ContentRequest;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import io.github.senagithub.tracker.content.dto.StatusChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    @Operation(summary = "List contents, optionally filtered; sorted by due date")
    public List<ContentResponse> list(
            @RequestParam(required = false) ContentStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) ContentType type,
            @Parameter(description = "Case-insensitive search in the title")
            @RequestParam(name = "q", required = false) String query) {
        return contentService.list(status, clientId, type, query);
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
