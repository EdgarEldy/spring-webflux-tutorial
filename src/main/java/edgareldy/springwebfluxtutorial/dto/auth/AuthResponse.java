package edgareldy.springwebfluxtutorial.dto.auth;

/**
 * Response returned by register/login, carrying the issued JWT. toString() masks token for the
 * same reason as the request DTOs in this package: the token is a bearer credential, equivalent
 * to a password for as long as it is valid, and LoggingAspect would otherwise log it in clear
 * text on every successful login.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record AuthResponse(
        String token,
        String username,
        String role
) {

    @Override
    public String toString() {
        return "AuthResponse{token=***, username='" + username + "', role='" + role + "'}";
    }
}
