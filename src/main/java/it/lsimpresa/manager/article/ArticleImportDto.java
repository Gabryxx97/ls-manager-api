package it.lsimpresa.manager.article;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

public final class ArticleImportDto {

    private ArticleImportDto() {
    }

    @Data
    public static class Request {

        @NotEmpty(message = "La richiesta deve contenere almeno un articolo")
        @Size(max = 5000, message = "Non è possibile importare più di 5.000 righe")
        private List<Item> items;
    }

    @Data
    public static class Item {

        private Integer rowNumber;
        private String sku;
        private String description;
        private String category;
        private String costCenter;
        private String unitOfMeasure;
        private String location;
        private BigDecimal stockQuantity;
        private BigDecimal unitPrice;
    }

    @Data
    public static class Response {

        private int received;
        private int created;
        private int updated;
        private int failed;
        private List<Error> errors = new ArrayList<>();
    }

    @Data
    public static class Error {

        private final Integer rowNumber;
        private final String field;
        private final String message;
    }
}
