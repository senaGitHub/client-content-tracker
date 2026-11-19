package io.github.senagithub.tracker.client;

import io.github.senagithub.tracker.client.dto.ClientRequest;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.common.FieldValidationException;
import io.github.senagithub.tracker.common.NotFoundException;
import io.github.senagithub.tracker.common.PageResponse;
import io.github.senagithub.tracker.common.Paging;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentRepository.OverdueCountRow;
import io.github.senagithub.tracker.content.ContentRepository.StatusCountRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ClientService {

    private final ClientRepository clientRepository;
    private final ContentRepository contentRepository;

    public ClientService(ClientRepository clientRepository, ContentRepository contentRepository) {
        this.clientRepository = clientRepository;
        this.contentRepository = contentRepository;
    }

    /** Properties the list endpoint may be sorted by (?sort=createdAt,desc). */
    private static final Set<String> SORTABLE = Set.of("name", "industry", "contactName", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by("name");

    /** All clients, unpaged: used by the dashboard summary. */
    public List<ClientResponse> list() {
        Map<Long, StatusCounts> counts = allCounts();
        return clientRepository.findAll(DEFAULT_SORT.and(Sort.by("id"))).stream()
                .map(client -> withCounts(client, counts))
                .toList();
    }

    /** Three queries plus the page count query, regardless of how many clients exist. */
    public PageResponse<ClientResponse> page(String query, Pageable pageable) {
        Page<Client> page = clientRepository.findAll(
                ClientSpecifications.matching(query),
                Paging.sanitize(pageable, DEFAULT_SORT, SORTABLE));
        Map<Long, StatusCounts> counts = allCounts();
        return PageResponse.from(page, client -> withCounts(client, counts));
    }

    private Map<Long, StatusCounts> allCounts() {
        return buildCounts(
                contentRepository.countByClientAndStatus(),
                contentRepository.countOverdueByClient(LocalDate.now()));
    }

    private static ClientResponse withCounts(Client client, Map<Long, StatusCounts> counts) {
        return ClientResponse.from(client, counts.getOrDefault(client.getId(), StatusCounts.EMPTY));
    }

    public ClientResponse get(Long id) {
        Client client = clientRepository.findById(id).orElseThrow(() -> new NotFoundException("Client", id));
        Map<Long, StatusCounts> counts = buildCounts(
                contentRepository.countByStatusForClient(id),
                contentRepository.countOverdueByClient(LocalDate.now()));
        return ClientResponse.from(client, counts.getOrDefault(id, StatusCounts.EMPTY));
    }

    @Transactional
    public ClientResponse create(ClientRequest request) {
        Client client = new Client(
                request.name().trim(),
                request.industry().trim(),
                request.contactName().trim(),
                request.contactEmail().trim(),
                validCover(request.coverImageUrl()));
        clientRepository.save(client);
        return ClientResponse.from(client, StatusCounts.EMPTY);
    }

    /** Blank means "no cover"; anything else must be one of the bundled photos. */
    private static String validCover(String coverImageUrl) {
        if (coverImageUrl == null || coverImageUrl.isBlank()) {
            return null;
        }
        String cover = coverImageUrl.trim();
        if (!CoverImages.ALL.contains(cover)) {
            throw new FieldValidationException("coverImageUrl",
                    "Cover must be one of the bundled photos: " + String.join(", ", CoverImages.ALL));
        }
        return cover;
    }

    private static Map<Long, StatusCounts> buildCounts(List<StatusCountRow> statusRows, List<OverdueCountRow> overdueRows) {
        Map<Long, Long> overdueByClient = overdueRows.stream()
                .collect(Collectors.toMap(OverdueCountRow::getClientId, OverdueCountRow::getTotal));

        Map<Long, long[]> perClient = new HashMap<>(); // [draft, inReview, published]
        for (StatusCountRow row : statusRows) {
            long[] values = perClient.computeIfAbsent(row.getClientId(), id -> new long[3]);
            values[row.getStatus().ordinal()] = row.getTotal();
        }

        Map<Long, StatusCounts> result = new HashMap<>();
        perClient.forEach((clientId, v) -> result.put(clientId, new StatusCounts(
                v[0], v[1], v[2],
                overdueByClient.getOrDefault(clientId, 0L),
                v[0] + v[1] + v[2])));
        return result;
    }
}
