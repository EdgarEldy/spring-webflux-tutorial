package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.entity.Category;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

/**
 * Reactive repository for Category. R2DBC repositories have no Pageable support, so pagination
 * is done through an explicit LIMIT/OFFSET query instead of a derived Page-returning method.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface CategoryRepository extends ReactiveCrudRepository<Category, Long> {

    @Query("SELECT * FROM categories ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Category> findAllPaged(long limit, long offset);
}
