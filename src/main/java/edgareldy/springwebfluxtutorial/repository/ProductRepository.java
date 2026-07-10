package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.entity.Product;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for Product. Pagination and category filtering are both done through
 * explicit LIMIT/OFFSET queries, since R2DBC repositories have no Pageable support and there is
 * no JPA-style join to a Category association to lean on.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    /**
     * All products currently linked to a category, used by CategoryServiceImpl.delete() to
     * check the business rule reactively (via Flux.hasElements()) rather than loading a
     * blocking collection just to test emptiness.
     */
    Flux<Product> findByCategoryId(Long categoryId);

    @Query("SELECT * FROM products ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Product> findAllPaged(long limit, long offset);

    @Query("SELECT * FROM products WHERE category_id = :categoryId ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Product> findByCategoryIdPaged(Long categoryId, long limit, long offset);

    Mono<Long> countByCategoryId(Long categoryId);
}
