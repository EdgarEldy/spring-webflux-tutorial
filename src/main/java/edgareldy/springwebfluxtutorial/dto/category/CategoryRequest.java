package edgareldy.springwebfluxtutorial.dto.category;

import jakarta.validation.constraints.NotBlank;

/**
 * Fields a client may submit to create or update a Category. Validated automatically by
 * @Valid on the annotated controller.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        String categoryName
) {
}
