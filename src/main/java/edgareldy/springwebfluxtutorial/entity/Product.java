package edgareldy.springwebfluxtutorial.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping the products table. Holds only the raw categoryId foreign key: unlike
 * the JPA sibling project, there is no ManyToOne association to Category, so the service layer
 * resolves it explicitly (flatMap on CategoryRepository) whenever a full Category is needed.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
@Table("products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    private Long id;

    @Column("category_id")
    private Long categoryId;

    @Column("product_name")
    private String productName;

    @Column("unit_price")
    private float unitPrice;
}
