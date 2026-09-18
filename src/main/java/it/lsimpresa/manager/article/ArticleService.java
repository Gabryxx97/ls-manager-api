package it.lsimpresa.manager.article;

import it.lsimpresa.manager.exception.ConflictException;
import it.lsimpresa.manager.exception.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Locale;
import java.util.Set;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ArticleService implements IArticleService {

    private final ArticleRepository repository;
    private final ArticleMapper mapper;
    private final Validator validator;

    @Override
    @Transactional
    public ArticleDto create(ArticleDto article) {
        ArticleJpa entity = new ArticleJpa();
        apply(article, entity, false);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ArticleDto update(Long id, ArticleDto article) {
        ArticleJpa entity = findEntity(id);
        apply(article, entity, false);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public ArticleDto find(Long id) {
        return mapper.toDto(findEntity(id));
    }

    @Override
    public ArticleImportDto.Response importArticles(ArticleImportDto.Request request) {
        ArticleImportDto.Response response = new ArticleImportDto.Response();
        response.setReceived(request.getItems().size());

        for (ArticleImportDto.Item item : request.getItems()) {
            ArticleDto article = new ArticleDto();
            article.setSku(item.getSku());
            article.setDescription(item.getDescription());
            article.setCategory(item.getCategory());
            article.setCostCenter(item.getCostCenter());
            article.setUnitOfMeasure(item.getUnitOfMeasure());
            article.setLocation(item.getLocation());
            article.setStockQuantity(item.getStockQuantity());
            article.setUnitPrice(item.getUnitPrice());

            if (item.getRowNumber() == null || item.getRowNumber() < 2) {
                response.getErrors().add(new ArticleImportDto.Error(
                        item.getRowNumber(), "rowNumber", "Il numero di riga deve essere almeno 2"));
                continue;
            }

            Set<ConstraintViolation<ArticleDto>> violations = validator.validate(article);
            if (!violations.isEmpty()) {
                violations.stream()
                        .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                        .forEach(violation -> response.getErrors().add(new ArticleImportDto.Error(
                                item.getRowNumber(), violation.getPropertyPath().toString(), violation.getMessage())));
                continue;
            }

            try {
                String sku = normalizeSku(article.getSku());
                ArticleJpa entity = repository.findBySkuIgnoreCase(sku).orElseGet(ArticleJpa::new);
                boolean updating = entity.getId() != null;
                apply(article, entity, true);
                repository.saveAndFlush(entity);
                if (updating) {
                    response.setUpdated(response.getUpdated() + 1);
                } else {
                    response.setCreated(response.getCreated() + 1);
                }
            } catch (RuntimeException exception) {
                response.getErrors().add(new ArticleImportDto.Error(
                        item.getRowNumber(), null, "Impossibile salvare l'articolo"));
            }
        }

        response.setFailed(response.getReceived() - response.getCreated() - response.getUpdated());
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ArticleJpa entity = findEntity(id);
        try {
            repository.delete(entity);
            repository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("L'articolo è utilizzato da uno o più ordini");
        }
    }

    private ArticleJpa findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Articolo non trovato"));
    }

    private void apply(ArticleDto source, ArticleJpa target, boolean preserveMissingPrice) {
        String sku = normalizeSku(source.getSku());
        if (target.getId() == null
                ? repository.existsBySkuIgnoreCase(sku)
                : repository.existsBySkuIgnoreCaseAndIdNot(sku, target.getId())) {
            throw new ConflictException("Lo SKU è già utilizzato");
        }
        target.setSku(sku);
        target.setDescription(normalizeRequired(source.getDescription()));
        target.setCategory(normalizeOptional(source.getCategory()));
        target.setCostCenter(normalizeOptional(source.getCostCenter()));
        target.setUnitOfMeasure(normalizeOptional(source.getUnitOfMeasure()));
        target.setLocation(normalizeOptional(source.getLocation()));
        target.setStockQuantity(normalizeQuantity(source.getStockQuantity()));
        if (!preserveMissingPrice || source.getUnitPrice() != null) {
            target.setUnitPrice(normalizePrice(source.getUnitPrice()));
        }
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeSku(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim().replaceAll("\\s+", "-").toUpperCase(Locale.ROOT);
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null) return null;
        if (value.signum() < 0 || value.scale() > 2) {
            throw new IllegalArgumentException("Il prezzo deve essere non negativo e avere al massimo due decimali");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null) return null;
        if (value.scale() > 2) {
            throw new IllegalArgumentException("La giacenza deve avere al massimo due decimali");
        }
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }
}
