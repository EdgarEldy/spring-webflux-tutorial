package edgareldy.springwebfluxtutorial.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * Fields a client submits to sign in. toString() masks password for the same reason as
 * RegisterRequest: AuthServiceImpl.login() takes this record as an argument, and LoggingAspect
 * would otherwise log the plaintext password on every login attempt.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record LoginRequest(
        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password
) {

    @Override
    public String toString() {
        return "LoginRequest{username='" + username + "', password=***}";
    }
}
