package edgareldy.springwebfluxtutorial.entity.user;

/**
 * The two roles this tutorial's authorization rules distinguish between: USER can read every
 * resource once authenticated, ADMIN can also write to categories/products/customers/orders.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public enum Role {
    USER,
    ADMIN
}
