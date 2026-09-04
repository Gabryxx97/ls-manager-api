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
import java.util.Set;

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
        apply(article, entity);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ArticleDto update(Long id, ArticleDto article) {
        ArticleJpa entity = findEntity(id);
        apply(article, entity);
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
            article.setName(item.getName());
            article.setDescription(item.getDescription());

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
                ArticleJpa entity = new ArticleJpa();
                apply(article, entity);
                repository.saveAndFlush(entity);
                response.setCreated(response.getCreated() + 1);
            } catch (RuntimeException exception) {
                response.getErrors().add(new ArticleImportDto.Error(
                        item.getRowNumber(), null, "Impossibile salvare l'articolo"));
            }
        }

        response.setFailed(response.getReceived() - response.getCreated());
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

    private void apply(ArticleDto source, ArticleJpa target) {
        target.setName(normalizeRequired(source.getName()));
        target.setDescription(normalizeOptional(source.getDescription()));
    }

    private String normalizeRequired(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
