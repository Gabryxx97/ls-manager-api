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

@Data
@Entity
@Table(name = "article")
public class ArticleJpa implements Searchable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "search", nullable = false, columnDefinition = "text")
    private String search;

    @Override
    @PrePersist
    @PreUpdate
    public void searchFill() {
        search = String.join(" ", name == null ? "" : name,
                description == null ? "" : description).trim();
    }
}
