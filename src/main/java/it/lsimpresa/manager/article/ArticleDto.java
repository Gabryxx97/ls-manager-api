package it.lsimpresa.manager.article;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

@Data
public class ArticleDto {

    private Long id;

    @NotBlank(message = "Lo SKU è obbligatorio")
    @Size(max = 64, message = "Lo SKU non può superare 64 caratteri")
    private String sku;

    @NotBlank(message = "La descrizione è obbligatoria")
    private String description;

    @Size(max = 64, message = "La categoria non può superare 64 caratteri")
    private String category;

    @Size(max = 128, message = "Il centro di costo non può superare 128 caratteri")
    private String costCenter;

    @Size(max = 16, message = "L'unità di misura non può superare 16 caratteri")
    private String unitOfMeasure;

    @Size(max = 255, message = "L'ubicazione non può superare 255 caratteri")
    private String location;

    @Digits(integer = 12, fraction = 2, message = "La giacenza deve avere al massimo due decimali")
    private BigDecimal stockQuantity;

    @DecimalMin(value = "0.00", message = "Il prezzo non può essere negativo")
    @Digits(integer = 10, fraction = 2, message = "Il prezzo deve avere al massimo due decimali")
    private BigDecimal unitPrice;
}
