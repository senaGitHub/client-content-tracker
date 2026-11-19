package io.github.senagithub.tracker.client;

import io.github.senagithub.tracker.client.dto.ClientRequest;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.github.senagithub.tracker.client.dto.StatusCounts;
import io.github.senagithub.tracker.common.NotFoundException;
import io.github.senagithub.tracker.content.ContentRepository;
import io.github.senagithub.tracker.content.ContentRepository.OverdueCountRow;
import io.github.senagithub.tracker.content.ContentRepository.StatusCountRow;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /** Three queries in total (clients, status counts, overdue counts), regardless of how many clients exist. */
    public List<ClientResponse> list() {
        Map<Long, StatusCounts> counts = buildCounts(
                contentRepository.countByClientAndStatus(),
                contentRepository.countOverdueByClient(LocalDate.now()));
        return clientRepository.findAll(Sort.by("name")).stream()
                .map(client -> ClientResponse.from(client, counts.getOrDefault(client.getId(), StatusCounts.EMPTY)))
                .toList();
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
                request.contactEmail().trim());
        clientRepository.save(client);
        return ClientResponse.from(client, StatusCounts.EMPTY);
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
