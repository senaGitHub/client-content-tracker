package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.client.Client;
import io.github.senagithub.tracker.client.ClientRepository;
import io.github.senagithub.tracker.common.FieldValidationException;
import io.github.senagithub.tracker.common.NotFoundException;
import io.github.senagithub.tracker.content.dto.ContentRequest;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import io.github.senagithub.tracker.common.PageResponse;
import io.github.senagithub.tracker.common.Paging;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ContentService {

    /** Properties the list endpoint may be sorted by (?sort=title,asc). */
    private static final Set<String> SORTABLE =
            Set.of("dueDate", "title", "status", "type", "createdAt", "updatedAt", "client.name");

    private final ContentRepository contentRepository;
    private final ClientRepository clientRepository;
    private final ActivityService activityService;

    public ContentService(ContentRepository contentRepository,
                          ClientRepository clientRepository,
                          ActivityService activityService) {
        this.contentRepository = contentRepository;
        this.clientRepository = clientRepository;
        this.activityService = activityService;
    }

    public PageResponse<ContentResponse> list(ContentFilter filter, Pageable pageable) {
        LocalDate today = LocalDate.now();
        Pageable page = Paging.sanitize(pageable, ContentSpecifications.BY_DUE_DATE, SORTABLE);
        return PageResponse.from(
                contentRepository.findAll(ContentSpecifications.matching(filter, today), page),
                content -> ContentResponse.from(content, today));
    }

    /** New content always starts as a draft; the pipeline is the only way forward. */
    @Transactional
    public ContentResponse create(ContentRequest request) {
        Content content = new Content(
                findClientForRequest(request.clientId()),
                request.title().trim(),
                request.type(),
                blankToNull(request.channel()),
                ContentStatus.DRAFT,
                request.dueDate(),
                blankToNull(request.notes()));
        contentRepository.save(content);
        activityService.record(content, ActivityAction.CREATED, null, null);
        return ContentResponse.from(content, LocalDate.now());
    }

    @Transactional
    public ContentResponse update(Long id, ContentRequest request) {
        Content content = findContent(id);
        content.update(
                findClientForRequest(request.clientId()),
                request.title().trim(),
                request.type(),
                blankToNull(request.channel()),
                request.dueDate(),
                blankToNull(request.notes()));
        // Flush now so @PreUpdate sets updatedAt before we build the response.
        contentRepository.saveAndFlush(content);
        activityService.record(content, ActivityAction.UPDATED, null, null);
        return ContentResponse.from(content, LocalDate.now());
    }

    @Transactional
    public ContentResponse changeStatus(Long id, ContentStatus target) {
        Content content = findContent(id);
        ContentStatus current = content.getStatus();
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(current, target);
        }
        content.changeStatus(target);
        contentRepository.saveAndFlush(content);
        activityService.record(content, ActivityAction.STATUS_CHANGED, current, target);
        return ContentResponse.from(content, LocalDate.now());
    }

    private Content findContent(Long id) {
        return contentRepository.findById(id).orElseThrow(() -> new NotFoundException("Content", id));
    }

    /** A missing client here is a bad field value (400), not a missing URL resource (404). */
    private Client findClientForRequest(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new FieldValidationException("clientId", "Client " + clientId + " does not exist."));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
