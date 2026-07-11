package edgareldy.springwebfluxtutorial.dto.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Fields a client may submit to create or update a Customer. Validated manually via an
 * injected Validator in CustomerHandler, since @Valid does not apply in the functional
 * endpoints style this resource uses.
 * <p>
 * Created by edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-webflux-tutorial
 */
public record CustomerRequest(
        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Telephone is required")
        String telephone,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email,

        @NotBlank(message = "Address is required")
        String address
) {
}
