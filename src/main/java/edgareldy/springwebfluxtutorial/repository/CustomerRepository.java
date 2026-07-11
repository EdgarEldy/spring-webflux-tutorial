package edgareldy.springwebfluxtutorial.repository;

import edgareldy.springwebfluxtutorial.entity.Customer;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reactive repository for Customer. Search and pagination are both explicit LIMIT/OFFSET
 * queries, since R2DBC repositories have no Pageable support.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

    /**
     * Used by CustomerServiceImpl to enforce the email uniqueness business rule, and to
     * exclude the customer's own current email when checking on update.
     */
    Mono<Customer> findByEmailIgnoreCase(String email);

    @Query("SELECT * FROM customers ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Customer> findAllPaged(long limit, long offset);

    @Query("SELECT * FROM customers WHERE first_name ILIKE CONCAT('%', :term, '%') "
            + "OR last_name ILIKE CONCAT('%', :term, '%') ORDER BY id LIMIT :limit OFFSET :offset")
    Flux<Customer> searchPaged(String term, long limit, long offset);

    @Query("SELECT COUNT(*) FROM customers WHERE first_name ILIKE CONCAT('%', :term, '%') "
            + "OR last_name ILIKE CONCAT('%', :term, '%')")
    Mono<Long> countSearch(String term);
}
