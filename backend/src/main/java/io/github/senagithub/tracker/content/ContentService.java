package io.github.senagithub.tracker.content;

import io.github.senagithub.tracker.activity.ActivityAction;
import io.github.senagithub.tracker.activity.ActivityService;
import io.github.senagithub.tracker.client.Client;
import io.github.senagithub.tracker.client.ClientRepository;
import io.github.senagithub.tracker.common.FieldValidationException;
import io.github.senagithub.tracker.common.NotFoundException;
import io.github.senagithub.tracker.content.dto.ContentRequest;
import io.github.senagithub.tracker.content.dto.ContentResponse;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContentService {

    private static final Sort DEFAULT_SORT = Sort.by("dueDate", "id");

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

    public List<ContentResponse> list(ContentStatus status, Long clientId, ContentType type, String query) {
        LocalDate today = LocalDate.now();
        return contentRepository.findAll(ContentSpecifications.matching(status, clientId, type, query), DEFAULT_SORT)
                .stream()
                .map(content -> ContentResponse.from(content, today))
                .toList();
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
