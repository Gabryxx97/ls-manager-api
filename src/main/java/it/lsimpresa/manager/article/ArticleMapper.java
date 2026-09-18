package it.lsimpresa.manager.article;

import it.lsimpresa.manager.utils.DtoMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleMapper implements DtoMapper<ArticleDto, ArticleJpa> {

    public ArticleDto toDto(ArticleJpa entity) {
        if (entity == null) return null;
        ArticleDto dto = new ArticleDto();
        dto.setId(entity.getId());
        dto.setSku(entity.getSku());
        dto.setDescription(entity.getDescription());
        dto.setCategory(entity.getCategory());
        dto.setCostCenter(entity.getCostCenter());
        dto.setUnitOfMeasure(entity.getUnitOfMeasure());
        dto.setLocation(entity.getLocation());
        dto.setStockQuantity(entity.getStockQuantity());
        dto.setUnitPrice(entity.getUnitPrice());
        return dto;
    }

    @Override
    public List<ArticleDto> map(List<ArticleJpa> articles) {
        return articles.stream().map(this::toDto).toList();
    }
}
