package it.lsimpresa.manager.order;

import it.lsimpresa.manager.article.ArticleJpa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(
        name = "order_detail",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_detail_order_article",
                columnNames = {"order_id", "article_id"}))
@Check(name = "ck_order_detail_quantities", constraints =
        "quantity > 0 AND picked_quantity >= 0 AND picked_quantity <= quantity " +
        "AND ((article_id IS NOT NULL AND custom_description IS NULL " +
        "AND custom_sku IS NULL AND custom_unit_of_measure IS NULL) " +
        "OR (article_id IS NULL AND custom_description IS NOT NULL AND unit_price IS NOT NULL))")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "article_id",
            foreignKey = @ForeignKey(name = "fk_order_detail_article"))
    private ArticleJpa article;

    @Column(name = "custom_description", columnDefinition = "text")
    private String customDescription;

    @Column(name = "custom_sku", length = 64)
    private String customSku;

    @Column(name = "custom_unit_of_measure", length = 16)
    private String customUnitOfMeasure;

    @NotNull
    @Positive
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private OrderDetailStatus status = OrderDetailStatus.TO_PICK;

    @NotNull
    @Column(name = "picked_quantity", nullable = false)
    private Integer pickedQuantity = 0;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    public boolean isCustom() {
        return article == null;
    }

    public String displaySku() {
        if (article != null) return article.getSku();
        return customSku == null || customSku.isBlank() ? "NON CENSITO" : customSku;
    }

    public String displayDescription() {
        return article != null ? article.getDescription() : customDescription;
    }
}
