package edgareldy.springwebfluxtutorial.repository;

/**
 * Read-only projection of an order joined with its customer and product, populated directly
 * from a SQL join in OrderRepository instead of resolving Customer/Product with a separate
 * flatMap per row, which would mean one extra query per order on a paginated list. Not a
 * persistent entity: OrderMapper converts every instance to OrderResponse before it reaches a
 * controller.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record OrderView(
        Long id,
        Long customerId,
        Long productId,
        int quantity,
        double total,
        String customerFirstName,
        String customerLastName,
        String productName
) {
}
