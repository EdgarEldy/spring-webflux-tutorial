package edgareldy.springwebfluxtutorial.mapper;

import edgareldy.springwebfluxtutorial.dto.order.OrderResponse;
import edgareldy.springwebfluxtutorial.entity.Customer;
import edgareldy.springwebfluxtutorial.entity.Order;
import edgareldy.springwebfluxtutorial.entity.Product;
import edgareldy.springwebfluxtutorial.repository.OrderView;
import org.mapstruct.Mapper;

/**
 * Mapper between Order and OrderResponse. Two overloads of toResponse cover the two ways this
 * project resolves the Customer/Product relations: toResponse(OrderView) for reads, where
 * OrderRepository already joined them in SQL; toResponse(Order, Customer, Product) for
 * create()/update(), where OrderServiceImpl already holds both resolved entities from a
 * Mono.zip. Written as default methods rather than declarative @Mapping annotations since
 * both target the same nested record types (CustomerSummary/ProductSummary) from different
 * source shapes.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Mapper(componentModel = "spring")
public interface OrderMapper {

    default OrderResponse toResponse(OrderView view) {
        return new OrderResponse(
                view.id(),
                new OrderResponse.CustomerSummary(view.customerId(), view.customerFirstName(), view.customerLastName()),
                new OrderResponse.ProductSummary(view.productId(), view.productName()),
                view.quantity(),
                view.total()
        );
    }

    default OrderResponse toResponse(Order order, Customer customer, Product product) {
        return new OrderResponse(
                order.getId(),
                new OrderResponse.CustomerSummary(customer.getId(), customer.getFirstName(), customer.getLastName()),
                new OrderResponse.ProductSummary(product.getId(), product.getProductName()),
                order.getQuantity(),
                order.getTotal()
        );
    }
}
