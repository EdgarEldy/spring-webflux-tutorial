package edgareldy.springwebfluxtutorial.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Fields a client submits to register a new account. toString() masks password: LoggingAspect
 * logs every service method's arguments via doOnNext()/doOnError() on the returned Mono, and
 * AuthServiceImpl.register() takes this record as an argument, so an unmasked toString() would
 * put the plaintext password straight into application logs on every registration.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record RegisterRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        String password,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        String email
) {

    @Override
    public String toString() {
        return "RegisterRequest{username='" + username + "', password=***, email='" + email + "'}";
    }
}
