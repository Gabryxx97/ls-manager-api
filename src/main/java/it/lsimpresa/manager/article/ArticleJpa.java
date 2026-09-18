package it.lsimpresa.manager.article;

import it.lsimpresa.manager.utils.search.Searchable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "article")
public class ArticleJpa implements Searchable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku", nullable = false, length = 64)
    private String sku;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "category", length = 64)
    private String category;

    @Column(name = "cost_center", length = 128)
    private String costCenter;

    @Column(name = "unit_of_measure", length = 16)
    private String unitOfMeasure;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "stock_quantity", precision = 14, scale = 2)
    private BigDecimal stockQuantity;

    @Column(name = "search", nullable = false, columnDefinition = "text")
    private String search;

    @Override
    @PrePersist
    @PreUpdate
    public void searchFill() {
        search = String.join(" ", sku == null ? "" : sku,
                description == null ? "" : description,
                category == null ? "" : category,
                costCenter == null ? "" : costCenter,
                unitOfMeasure == null ? "" : unitOfMeasure,
                location == null ? "" : location).trim().replaceAll("\\s+", " ");
    }
}
