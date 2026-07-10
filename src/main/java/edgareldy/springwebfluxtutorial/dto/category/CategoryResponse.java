package edgareldy.springwebfluxtutorial.dto.category;

/**
 * Category representation exposed to API clients.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record CategoryResponse(
        Long id,
        String categoryName
) {
}
