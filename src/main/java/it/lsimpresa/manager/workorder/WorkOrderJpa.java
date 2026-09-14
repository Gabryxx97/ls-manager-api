package it.lsimpresa.manager.workorder;

import it.lsimpresa.manager.utils.search.Searchable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Getter
@Setter
@Entity
@Table(name = "work_order", uniqueConstraints =
        @UniqueConstraint(name = "uk_work_order_name_key", columnNames = "name_key"))
public class WorkOrderJpa implements Searchable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "search", nullable = false, columnDefinition = "text")
    private String search;

    @Column(name = "name_key", nullable = false, length = 255)
    private String nameKey;

    @Override
    @PrePersist
    @PreUpdate
    public void searchFill() {
        name = normalizeName(name);
        nameKey = name.toLowerCase(Locale.ROOT);
        search = String.join(" ", name, description == null ? "" : description).trim();
    }

    public static String normalizeName(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }
}
