package it.lsimpresa.manager.order;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'USER_ROLE')")
public class OrderController extends SearchControllerBase {
    private static final Map<String, Set<String>> ALLOWED_FILTERS = Map.of(
            "id", Set.of("EQUAL", "IN"),
            "search", Set.of("ILIKE"),
            "date", Set.of("EQUAL", "BETWEEN", "GTE", "LTE"),
            "priority", Set.of("EQUAL", "IN"),
            "status", Set.of("EQUAL", "IN"));
    private static final Set<String> ALLOWED_SORTS = Set.of(
            "id", "name", "date", "priority", "status");

    private final ISearchService searchService;
    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @GetMapping
    public ResponseEntity<List<OrderSummaryDto>> search(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        Page<OrderSummaryDto> results = searchService.search(
                filter(filter, ALLOWED_FILTERS), sort(sort, ALLOWED_SORTS),
                pageable(page, pageSize), OrderJpa.class, orderMapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> get(@PathVariable Long id) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.findDetailed(id)));
    }

    @PostMapping
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderRequestDto request) {
        OrderJpa created = orderService.create(orderMapper.toDraft(request));
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId()))
                .body(orderMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> update(@PathVariable Long id,
                                            @Valid @RequestBody OrderRequestDto request) {
        return ResponseEntity.ok(orderMapper.toDto(
                orderService.update(id, orderMapper.toDraft(request))));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        orderService.delete(id);
    }
}
