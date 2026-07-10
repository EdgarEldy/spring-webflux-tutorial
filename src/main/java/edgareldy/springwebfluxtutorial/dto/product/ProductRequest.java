package edgareldy.springwebfluxtutorial.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Fields a client may submit to create or update a Product. Validated automatically by @Valid
 * on the annotated controller.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record ProductRequest(
        @NotNull(message = "Category id is required")
        Long categoryId,

        @NotBlank(message = "Product name is required")
        String productName,

        @Positive(message = "Unit price must be positive")
        float unitPrice
) {
}
