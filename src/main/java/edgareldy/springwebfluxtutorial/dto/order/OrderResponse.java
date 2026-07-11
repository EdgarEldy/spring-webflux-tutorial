package edgareldy.springwebfluxtutorial.dto.order;

/**
 * Order representation exposed to API clients, with Customer and Product resolved and
 * embedded as summaries rather than raw ids.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record OrderResponse(
        Long id,
        CustomerSummary customer,
        ProductSummary product,
        int quantity,
        double total
) {

    public record CustomerSummary(Long id, String firstName, String lastName) {
    }

    public record ProductSummary(Long id, String productName) {
    }
}
