package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import it.lsimpresa.manager.article.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private OrderService service;

    @Test
    void createsAggregateWithNormalizedNameAndLinkedDetails() {
        ArticleJpa article = article(7L);
        when(articleRepository.findAllById(any())).thenReturn(List.of(article));
        when(orderRepository.save(org.mockito.ArgumentMatchers.any(OrderJpa.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderJpa created = service.create(draft(List.of(new OrderDraft.Detail(7L, 3))));

        assertThat(created.getName()).isEqualTo("Ordine urgente");
        assertThat(created.getDetails()).hasSize(1);
        assertThat(created.getDetails().getFirst().getOrder()).isSameAs(created);
        assertThat(created.getDetails().getFirst().getArticle()).isSameAs(article);
        assertThat(created.getDetails().getFirst().getQuantity()).isEqualTo(3);
    }

    @Test
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> service.create(draft(List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("L'ordine deve contenere almeno una riga");
    }

    @Test
    void rejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> service.create(draft(List.of(new OrderDraft.Detail(7L, 0)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("La quantità deve essere maggiore di zero");
    }

    @Test
    void rejectsDuplicateArticle() {
        assertThatThrownBy(() -> service.create(draft(List.of(
                new OrderDraft.Detail(7L, 1),
                new OrderDraft.Detail(7L, 2)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Lo stesso articolo non può comparire più volte nell'ordine");
    }

    @Test
    void rejectsUnknownArticle() {
        when(articleRepository.findAllById(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.create(draft(List.of(new OrderDraft.Detail(7L, 1)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uno o più articoli non esistono");
    }

    @Test
    void deletesExistingOrder() {
        OrderJpa order = new OrderJpa();
        when(orderRepository.findById(12L)).thenReturn(java.util.Optional.of(order));

        service.delete(12L);

        verify(orderRepository).delete(order);
    }

    private OrderDraft draft(List<OrderDraft.Detail> details) {
        return new OrderDraft(
                "  Ordine   urgente ",
                LocalDate.of(2026, 9, 5),
                OrderPriority.HIGH,
                OrderStatus.DRAFT,
                details);
    }

    private ArticleJpa article(Long id) {
        ArticleJpa article = new ArticleJpa();
        article.setId(id);
        article.setName("Articolo");
        return article;
    }
}
