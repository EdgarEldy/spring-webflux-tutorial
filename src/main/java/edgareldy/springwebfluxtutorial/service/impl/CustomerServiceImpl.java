package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerRequest;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerResponse;
import edgareldy.springwebfluxtutorial.entity.Customer;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CustomerMapper;
import edgareldy.springwebfluxtutorial.repository.CustomerRepository;
import edgareldy.springwebfluxtutorial.service.CustomerService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

/**
 * Reactive implementation of CustomerService. Email uniqueness (case-insensitive) is enforced
 * as a business rule on both create() and update(), consistently comparing with
 * findByEmailIgnoreCase so the two methods cannot drift the way a create()-only check would.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerRepository customerRepository, CustomerMapper customerMapper) {
        this.customerRepository = customerRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public Mono<CustomerResponse> findById(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id " + id)))
                .map(customerMapper::toResponse);
    }

    @Override
    public Mono<PageResponse<CustomerResponse>> findAll(int page, int size, String search) {
        long offset = (long) page * size;
        boolean hasSearch = search != null && !search.isBlank();

        var contentFlux = hasSearch
                ? customerRepository.searchPaged(search, size, offset)
                : customerRepository.findAllPaged(size, offset);
        var countMono = hasSearch
                ? customerRepository.countSearch(search)
                : customerRepository.count();

        Mono<List<CustomerResponse>> contentMono = contentFlux
                .map(customerMapper::toResponse)
                .collectList();

        return Mono.zip(contentMono, countMono)
                .map(tuple -> PageResponse.of(tuple.getT1(), page, size, tuple.getT2()));
    }

    /**
     * findByEmailIgnoreCase() feeds a flatMap that errors when a customer already owns that
     * email, with switchIfEmpty() as the actual save path: the save only runs when the lookup
     * found nobody, so the error and the write can never both happen for the same request.
     */
    @Override
    @Transactional
    public Mono<CustomerResponse> create(CustomerRequest request) {
        return customerRepository.findByEmailIgnoreCase(request.email())
                .flatMap(existing -> Mono.<Customer>error(
                        new BusinessRuleException("Email " + request.email() + " is already in use")))
                .switchIfEmpty(Mono.defer(() -> customerRepository.save(customerMapper.toEntity(request))))
                .map(customerMapper::toResponse);
    }

    /**
     * Same email-uniqueness pattern as create(), but the match is filtered out when it is the
     * customer's own current row, otherwise updating a customer without changing their email
     * would always report a conflict with themselves.
     */
    @Override
    @Transactional
    public Mono<CustomerResponse> update(Long id, CustomerRequest request) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id " + id)))
                .flatMap(existing -> customerRepository.findByEmailIgnoreCase(request.email())
                        .filter(other -> !other.getId().equals(id))
                        .flatMap(other -> Mono.<Customer>error(
                                new BusinessRuleException("Email " + request.email() + " is already in use")))
                        .switchIfEmpty(Mono.defer(() -> {
                            existing.setFirstName(request.firstName());
                            existing.setLastName(request.lastName());
                            existing.setTelephone(request.telephone());
                            existing.setEmail(request.email());
                            existing.setAddress(request.address());
                            return customerRepository.save(existing);
                        })))
                .map(customerMapper::toResponse);
    }

    @Override
    @Transactional
    public Mono<Void> delete(Long id) {
        return customerRepository.findById(id)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Customer not found with id " + id)))
                .flatMap(existing -> customerRepository.deleteById(id));
    }
}
