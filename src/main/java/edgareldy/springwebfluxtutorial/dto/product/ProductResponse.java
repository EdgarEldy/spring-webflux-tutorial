package edgareldy.springwebfluxtutorial.dto.product;

import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;

/**
 * Product representation exposed to API clients, with its Category resolved and embedded
 * rather than left as a raw categoryId, since resolving that relation is the whole point of
 * ProductServiceImpl on a project with no automatic entity associations.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record ProductResponse(
        Long id,
        CategoryResponse category,
        String productName,
        float unitPrice
) {
}
