package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.utils.DtoMapper;
import org.springframework.stereotype.Component;
import java.util.List;

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
        dto.setDetails(entity.getDetails().stream().map(this::toDetail).toList());
        return dto;
    }

    public OrderDraft toDraft(OrderRequestDto request) {
        List<OrderDraft.Detail> details = request.getDetails().stream()
                .map(detail -> new OrderDraft.Detail(detail.getArticleId(), detail.getQuantity()))
                .toList();
        return new OrderDraft(request.getWorkOrderId(), request.getDate(), request.getPriority(),
                request.getStatus(), details);
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
        dto.setArticleName(article.getName());
        dto.setArticleDescription(article.getDescription());
        dto.setQuantity(entity.getQuantity());
        return dto;
    }
}
