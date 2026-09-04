package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;

@Getter
@Setter
@Entity
@Table(
        name = "order_detail",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_detail_order_article",
                columnNames = {"order_id", "article_id"}))
@Check(name = "ck_order_detail_quantity_positive", constraints = "quantity > 0")
public class OrderDetailJpa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "order_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_detail_order"))
    private OrderJpa order;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "article_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_order_detail_article"))
    private ArticleJpa article;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
