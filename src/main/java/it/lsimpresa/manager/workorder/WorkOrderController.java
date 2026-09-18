package it.lsimpresa.manager.workorder;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workorders")
@PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'OPERATOR_ROLE', 'WAREHOUSE_ROLE')")
public class WorkOrderController extends SearchControllerBase {
    private static final Map<String, Set<String>> ALLOWED_FILTERS = Map.of(
            "id", Set.of("EQUAL", "IN"), "search", Set.of("ILIKE"));
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "name", "description");

    private final ISearchService searchService;
    private final WorkOrderMapper mapper;
    private final IWorkOrderService service;

    @GetMapping
    public ResponseEntity<List<WorkOrderDto>> search(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        Page<WorkOrderDto> results = searchService.search(
                filter(filter, ALLOWED_FILTERS), sort(sort, ALLOWED_SORTS),
                pageable(page, pageSize), WorkOrderJpa.class, mapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.find(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_ROLE')")
    public ResponseEntity<WorkOrderDto> create(@Valid @RequestBody WorkOrderDto workOrder) {
        WorkOrderDto created = service.create(workOrder);
        return ResponseEntity.created(URI.create("/api/workorders/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_ROLE')")
    public ResponseEntity<WorkOrderDto> update(@PathVariable Long id,
                                                @Valid @RequestBody WorkOrderDto workOrder) {
        return ResponseEntity.ok(service.update(id, workOrder));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_ROLE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
