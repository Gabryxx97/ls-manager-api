package it.lsimpresa.manager.order;

import it.lsimpresa.manager.utils.search.ISearchService;
import it.lsimpresa.manager.utils.search.SearchControllerBase;
import it.lsimpresa.manager.utils.UserPrincipal;
import it.lsimpresa.manager.exception.ResourceNotAccessExeption;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'OPERATOR_ROLE', 'WAREHOUSE_ROLE')")
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
    private final OrderExportService orderExportService;
    private final UserPrincipal userPrincipal;

    @GetMapping
    public ResponseEntity<List<OrderSummaryDto>> search(
            @RequestParam("page") Integer page,
            @RequestParam("pageSize") Integer pageSize,
            @RequestParam(value = "filter", required = false) String filter,
            @RequestParam(value = "sort", required = false) String sort) {
        Map<String, it.lsimpresa.manager.utils.search.Operator> filters =
                new LinkedHashMap<>(filter(filter, ALLOWED_FILTERS));
        if (isOperator()) {
            it.lsimpresa.manager.utils.search.Operator ownerFilter =
                    new it.lsimpresa.manager.utils.search.Operator();
            ownerFilter.setOperator("EQUAL");
            ownerFilter.setValues(new Object[]{currentUserId()});
            filters.put("createdById", ownerFilter);
        }
        Page<OrderSummaryDto> results = searchService.search(
                filters, sort(sort, ALLOWED_SORTS),
                pageable(page, pageSize), OrderJpa.class, orderMapper);
        return response(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> get(@PathVariable Long id) {
        OrderJpa order = orderService.findDetailed(id);
        assertCanAccess(order);
        return ResponseEntity.ok(orderMapper.toDto(order));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable Long id,
                                         @RequestParam ExportScope scope,
                                         @RequestParam ExportFormat format) {
        OrderExportService.ExportDocument document = orderExportService.export(
                id, scope, format, userPrincipal.findUserByPrincipal(), isAdmin());
        return ResponseEntity.ok()
                .contentType(document.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.filename() + "\"")
                .body(document.content());
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'OPERATOR_ROLE', 'WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> create(@Valid @RequestBody OrderRequestDto request) {
        OrderJpa created = orderService.create(orderMapper.toDraft(request), userPrincipal.findUserByPrincipal(), isAdmin());
        return ResponseEntity.created(URI.create("/api/orders/" + created.getId()))
                .body(orderMapper.toDto(created));
    }

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAuthority('WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> claim(@PathVariable Long id) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.claim(id, userPrincipal.findUserByPrincipal())));
    }

    @PutMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('ADMIN_ROLE')")
    public ResponseEntity<OrderDto> assign(@PathVariable Long id,
                                            @Valid @RequestBody OrderAssigneeRequestDto request) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.assign(id, request.warehouseUserId())));
    }

    @DeleteMapping("/{id}/assignee")
    @PreAuthorize("hasAuthority('ADMIN_ROLE')")
    public ResponseEntity<OrderDto> revokeAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.revokeAssignment(id)));
    }

    @PutMapping("/{orderId}/details/{detailId}/picked-quantity")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> updatePickedQuantity(
            @PathVariable Long orderId,
            @PathVariable Long detailId,
            @Valid @RequestBody PickedQuantityRequestDto request) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.updatePickedQuantity(
                orderId, detailId, request.getPickedQuantity(),
                userPrincipal.findUserByPrincipal(), isAdmin())));
    }

    @PostMapping("/{orderId}/details/{detailId}/to-purchase")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> markToPurchase(
            @PathVariable Long orderId,
            @PathVariable Long detailId,
            @Valid @RequestBody PickedQuantityRequestDto request) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.markToPurchase(
                orderId, detailId, request.getPickedQuantity(),
                userPrincipal.findUserByPrincipal(), isAdmin())));
    }

    @PostMapping("/{orderId}/details/{detailId}/resume-picking")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> resumePicking(@PathVariable Long orderId,
                                                   @PathVariable Long detailId) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.resumePicking(
                orderId, detailId, userPrincipal.findUserByPrincipal(), isAdmin())));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'WAREHOUSE_ROLE')")
    public ResponseEntity<OrderDto> complete(@PathVariable Long id) {
        return ResponseEntity.ok(orderMapper.toDto(orderService.complete(
                id, userPrincipal.findUserByPrincipal(), isAdmin())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'OPERATOR_ROLE')")
    public ResponseEntity<OrderDto> update(@PathVariable Long id,
                                            @Valid @RequestBody OrderRequestDto request) {
        OrderJpa order = orderService.findEntity(id);
        assertCanAccess(order);
        return ResponseEntity.ok(orderMapper.toDto(orderService.update(id, orderMapper.toDraft(request))));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN_ROLE', 'OPERATOR_ROLE')")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        OrderJpa order = orderService.findEntity(id);
        assertCanAccess(order);
        orderService.delete(id);
    }

    private boolean isOperator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> "OPERATOR_ROLE".equals(granted.getAuthority()));
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> "ADMIN_ROLE".equals(granted.getAuthority()));
    }

    private Long currentUserId() {
        return userPrincipal.findUserByPrincipal().getId();
    }

    private void assertCanAccess(OrderJpa order) {
        if (isOperator() && (order.getCreatedBy() == null
                || !order.getCreatedBy().getId().equals(currentUserId()))) {
            throw new ResourceNotAccessExeption("Non hai i permessi per accedere a questo ordine");
        }
    }
}
