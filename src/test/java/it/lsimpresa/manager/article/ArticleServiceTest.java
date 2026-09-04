package it.lsimpresa.manager.article;

import it.lsimpresa.manager.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepository repository;

    @Mock
    private ArticleMapper mapper;

    @InjectMocks
    private ArticleService service;

    @Test
    void referencedArticleCannotBeDeleted() {
        ArticleJpa article = new ArticleJpa();
        article.setId(42L);
        when(repository.findById(42L)).thenReturn(Optional.of(article));
        doThrow(new DataIntegrityViolationException("foreign key"))
                .when(repository).flush();

        assertThatThrownBy(() -> service.delete(42L))
                .isInstanceOf(ConflictException.class)
                .hasMessage("L'articolo è utilizzato da uno o più ordini");
    }
}
