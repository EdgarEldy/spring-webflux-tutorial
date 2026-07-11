package edgareldy.springwebfluxtutorial.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Fields a client may submit to create or update an Order. total is never part of this
 * record: it is always computed server-side from the resolved Product's unit price, never
 * trusted from the client.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record OrderRequest(
        @NotNull(message = "Customer id is required")
        Long customerId,

        @NotNull(message = "Product id is required")
        Long productId,

        @Positive(message = "Quantity must be positive")
        int quantity
) {
}
