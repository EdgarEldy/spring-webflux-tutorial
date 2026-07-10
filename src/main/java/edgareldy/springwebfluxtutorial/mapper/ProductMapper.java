package edgareldy.springwebfluxtutorial.mapper;

import edgareldy.springwebfluxtutorial.dto.category.CategoryResponse;
import edgareldy.springwebfluxtutorial.dto.product.ProductRequest;
import edgareldy.springwebfluxtutorial.dto.product.ProductResponse;
import edgareldy.springwebfluxtutorial.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper between Product and its DTOs. toResponse takes the already-resolved
 * CategoryResponse as a second parameter instead of resolving it itself, since the mapper
 * stays synchronous and ProductServiceImpl is the one resolving the Category via flatMap.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    Product toEntity(ProductRequest request);

    @Mapping(target = "id", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unitPrice", source = "product.unitPrice")
    @Mapping(target = "category", source = "category")
    ProductResponse toResponse(Product product, CategoryResponse category);
}
