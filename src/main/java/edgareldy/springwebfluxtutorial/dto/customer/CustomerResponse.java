package edgareldy.springwebfluxtutorial.dto.customer;

/**
 * Customer representation exposed to API clients.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String telephone,
        String email,
        String address
) {
}
