package io.github.senagithub.tracker.client;

import io.github.senagithub.tracker.client.dto.ClientRequest;
import io.github.senagithub.tracker.client.dto.ClientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.github.senagithub.tracker.common.PageResponse;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/clients")
@Tag(name = "Clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "List clients with content counts per status (and overdue); paged, sorted by name",
            description = "Sortable by name, industry, contactName, createdAt. "
                    + "Page size defaults to 20 and is capped at 100.")
    public PageResponse<ClientResponse> list(
            @Parameter(description = "Case-insensitive search in name, industry and contact name")
            @RequestParam(name = "q", required = false) String query,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return clientService.page(query, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one client with its content counts")
    public ClientResponse get(@PathVariable Long id) {
        return clientService.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a client")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        ClientResponse created = clientService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edit a client",
            description = "Replaces name, industry, contact and cover. Same validation as POST "
                    + "(400 with field messages); 404 if the client doesn't exist.")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return clientService.update(id, request);
    }
}
