package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.user.UserJpa;
import it.lsimpresa.manager.utils.DtoMapper;
import org.springframework.stereotype.Component;
import java.util.List;
import java.math.BigDecimal;

@Component
public class OrderMapper implements DtoMapper<OrderSummaryDto, OrderJpa> {
    public OrderSummaryDto toSummary(OrderJpa entity) {
        OrderSummaryDto dto = new OrderSummaryDto();
        dto.setId(entity.getId());
        dto.setWorkOrderId(entity.getWorkOrder().getId());
        dto.setName(entity.getName());
        dto.setDate(entity.getDate());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setCategory(entity.getCategory());
        dto.setTakenInChargeAt(entity.getTakenInChargeAt());
        fillAssignedWarehouse(entity, dto);
        return dto;
    }

    public OrderDto toDto(OrderJpa entity) {
        OrderDto dto = new OrderDto();
        dto.setId(entity.getId());
        dto.setWorkOrderId(entity.getWorkOrder().getId());
        dto.setName(entity.getName());
        dto.setDate(entity.getDate());
        dto.setPriority(entity.getPriority());
        dto.setStatus(entity.getStatus());
        dto.setCategory(entity.getCategory());
        dto.setNotes(entity.getNotes());
        dto.setTakenInChargeAt(entity.getTakenInChargeAt());
        fillAssignedWarehouse(entity, dto);
        dto.setDetails(entity.getDetails().stream().map(this::toDetail).toList());
        if (entity.getDetails().stream().allMatch(detail -> detail.getUnitPrice() != null)) {
            dto.setNetTotal(entity.getDetails().stream()
                    .map(detail -> detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2));
        }
        return dto;
    }

    public OrderDraft toDraft(OrderRequestDto request) {
        List<OrderDraft.Detail> details = request.getDetails().stream()
                .map(detail -> new OrderDraft.Detail(detail.getArticleId(), detail.getQuantity()))
                .toList();
        return new OrderDraft(request.getWorkOrderId(), request.getDate(), request.getPriority(),
                request.getCategory(), request.getNotes(), details);
    }

    @Override
    public List<OrderSummaryDto> map(List<OrderJpa> orders) {
        return orders.stream().map(this::toSummary).toList();
    }

    private OrderDto.Detail toDetail(OrderDetailJpa entity) {
        ArticleJpa article = entity.getArticle();
        OrderDto.Detail dto = new OrderDto.Detail();
        dto.setId(entity.getId());
        dto.setArticleId(article.getId());
        dto.setArticleSku(article.getSku());
        dto.setArticleDescription(article.getDescription());
        dto.setQuantity(entity.getQuantity());
        dto.setStatus(entity.getStatus());
        dto.setPickedQuantity(entity.getPickedQuantity());
        dto.setRemainingQuantity(entity.getQuantity() - entity.getPickedQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        if (entity.getUnitPrice() != null) {
            dto.setSubtotal(entity.getUnitPrice().multiply(BigDecimal.valueOf(entity.getQuantity())).setScale(2));
        }
        return dto;
    }

    private void fillAssignedWarehouse(OrderJpa entity, OrderSummaryDto dto) {
        UserJpa warehouse = entity.getAssignedWarehouse();
        if (warehouse == null) return;
        dto.setAssignedWarehouseId(warehouse.getId());
        dto.setAssignedWarehouseName(displayName(warehouse));
    }

    private void fillAssignedWarehouse(OrderJpa entity, OrderDto dto) {
        UserJpa warehouse = entity.getAssignedWarehouse();
        if (warehouse == null) return;
        dto.setAssignedWarehouseId(warehouse.getId());
        dto.setAssignedWarehouseName(displayName(warehouse));
    }

    private String displayName(UserJpa user) {
        String fullName = String.join(" ", user.getName() == null ? "" : user.getName(),
                user.getSurname() == null ? "" : user.getSurname()).trim();
        return fullName.isBlank() ? user.getUsername() : fullName;
    }
}
