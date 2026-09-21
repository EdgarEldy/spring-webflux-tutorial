package edgareldy.springwebfluxtutorial.service.impl;

import edgareldy.springwebfluxtutorial.dto.common.PageResponse;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerRequest;
import edgareldy.springwebfluxtutorial.dto.customer.CustomerResponse;
import edgareldy.springwebfluxtutorial.entity.Customer;
import edgareldy.springwebfluxtutorial.exception.BusinessRuleException;
import edgareldy.springwebfluxtutorial.exception.ResourceNotFoundException;
import edgareldy.springwebfluxtutorial.mapper.CustomerMapper;
import edgareldy.springwebfluxtutorial.repository.CustomerRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.when;

/**
 * Unit tests for CustomerServiceImpl, in particular the email uniqueness business rule on
 * create() and update() (case-insensitive, and excluding the customer's own row on update).
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private CustomerMapper customerMapper;

    private CustomerServiceImpl customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, customerMapper);
    }

    @Test
    void _01_ShouldReturnMappedResponse_WhenCustomerIsFound() {
        Customer customer = customer(1L, "ada@example.com");
        CustomerResponse response = response(1L, "ada@example.com");
        when(customerRepository.findById(1L)).thenReturn(Mono.just(customer));
        when(customerMapper.toResponse(customer)).thenReturn(response);

        StepVerifier.create(customerService.findById(1L))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void _02_ShouldErrorWithResourceNotFound_WhenCustomerIsMissing() {
        when(customerRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(customerService.findById(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _03_ShouldZipPagedContentWithTotalCount_WhenAllCustomersAreRequested() {
        Customer customer = customer(1L, "ada@example.com");
        CustomerResponse response = response(1L, "ada@example.com");
        when(customerRepository.findAllPaged(20, 0)).thenReturn(Flux.just(customer));
        when(customerRepository.count()).thenReturn(Mono.just(1L));
        when(customerMapper.toResponse(customer)).thenReturn(response);

        StepVerifier.create(customerService.findAll(0, 20, null))
                .expectNext(PageResponse.of(List.of(response), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void _04_ShouldSearchByName_WhenSearchTermIsProvided() {
        Customer customer = customer(1L, "ada@example.com");
        CustomerResponse response = response(1L, "ada@example.com");
        when(customerRepository.searchPaged("lovelace", 20, 0)).thenReturn(Flux.just(customer));
        when(customerRepository.countSearch("lovelace")).thenReturn(Mono.just(1L));
        when(customerMapper.toResponse(customer)).thenReturn(response);

        StepVerifier.create(customerService.findAll(0, 20, "lovelace"))
                .expectNext(PageResponse.of(List.of(response), 0, 20, 1L))
                .verifyComplete();
    }

    @Test
    void _05_ShouldSaveCustomer_WhenEmailIsNotTaken() {
        CustomerRequest request = request("ada@example.com");
        Customer toSave = customer(null, "ada@example.com");
        Customer saved = customer(1L, "ada@example.com");
        CustomerResponse response = response(1L, "ada@example.com");
        when(customerRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Mono.empty());
        when(customerMapper.toEntity(request)).thenReturn(toSave);
        when(customerRepository.save(toSave)).thenReturn(Mono.just(saved));
        when(customerMapper.toResponse(saved)).thenReturn(response);

        StepVerifier.create(customerService.create(request))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void _06_ShouldErrorWithBusinessRule_WhenEmailIsAlreadyTaken() {
        CustomerRequest request = request("ada@example.com");
        Customer existing = customer(5L, "ada@example.com");
        when(customerRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Mono.just(existing));

        StepVerifier.create(customerService.create(request))
                .expectError(BusinessRuleException.class)
                .verify();
    }

    @Test
    void _07_ShouldErrorWithResourceNotFound_WhenUpdatedCustomerIsMissing() {
        when(customerRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(customerService.update(1L, request("ada@example.com")))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _08_ShouldAllowUpdate_WhenCustomerKeepsOwnEmail() {
        Customer existing = customer(1L, "ada@example.com");
        Customer updated = customer(1L, "ada@example.com");
        CustomerResponse response = response(1L, "ada@example.com");
        when(customerRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(customerRepository.findByEmailIgnoreCase("ada@example.com")).thenReturn(Mono.just(existing));
        when(customerRepository.save(existing)).thenReturn(Mono.just(updated));
        when(customerMapper.toResponse(updated)).thenReturn(response);

        StepVerifier.create(customerService.update(1L, request("ada@example.com")))
                .expectNext(response)
                .verifyComplete();
    }

    @Test
    void _09_ShouldErrorWithBusinessRule_WhenEmailBelongsToSomeoneElse() {
        Customer existing = customer(1L, "ada@example.com");
        Customer other = customer(2L, "grace@example.com");
        when(customerRepository.findById(1L)).thenReturn(Mono.just(existing));
        when(customerRepository.findByEmailIgnoreCase("grace@example.com")).thenReturn(Mono.just(other));

        StepVerifier.create(customerService.update(1L, request("grace@example.com")))
                .expectError(BusinessRuleException.class)
                .verify();
    }

    @Test
    void _10_ShouldErrorWithResourceNotFound_WhenDeletedCustomerIsMissing() {
        when(customerRepository.findById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(customerService.delete(1L))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void _11_ShouldDeleteCustomer_WhenCustomerIsFound() {
        Customer customer = customer(1L, "ada@example.com");
        when(customerRepository.findById(1L)).thenReturn(Mono.just(customer));
        when(customerRepository.deleteById(1L)).thenReturn(Mono.empty());

        StepVerifier.create(customerService.delete(1L))
                .verifyComplete();
    }

    private Customer customer(Long id, String email) {
        return Customer.builder()
                .id(id)
                .firstName("Ada")
                .lastName("Lovelace")
                .telephone("555-0100")
                .email(email)
                .address("1 Main St")
                .build();
    }

    private CustomerResponse response(Long id, String email) {
        return new CustomerResponse(id, "Ada", "Lovelace", "555-0100", email, "1 Main St");
    }

    private CustomerRequest request(String email) {
        return new CustomerRequest("Ada", "Lovelace", "555-0100", email, "1 Main St");
    }
}
