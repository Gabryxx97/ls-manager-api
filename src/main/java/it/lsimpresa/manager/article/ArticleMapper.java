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
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        return dto;
    }

    @Override
    public List<ArticleDto> map(List<ArticleJpa> articles) {
        return articles.stream().map(this::toDto).toList();
    }
}
