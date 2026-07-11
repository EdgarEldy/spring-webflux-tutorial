package edgareldy.springwebfluxtutorial.dto.auth;

/**
 * Response returned by GET /api/v1/auth/me, the currently authenticated user's profile. No
 * sensitive field to mask here, unlike the other DTOs in this package: no password, no token.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
public record UserProfileResponse(
        String username,
        String email,
        String role
) {
}
