package edgareldy.springwebfluxtutorial.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

/**
 * R2DBC entity mapping the app_users table. toString() is overridden to mask the password
 * hash: LoggingAspect logs every service method's arguments and return value via
 * doOnNext()/doOnError() on the returned Mono/Flux, and AuthServiceImpl's methods pass this
 * entity around, so an unmasked toString() would leak the password hash into application logs
 * on every login/register call.
 * <p>
 * Created by edgar.muhamyangabo on 7/11/26
 * Author : edgar.muhamyangabo
 * Date : 7/11/26
 * Project : spring-webflux-tutorial
 */
@Table("app_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppUser {

    @Id
    private Long id;

    private String username;

    private String password;

    private String email;

    private Role role;

    @Override
    public String toString() {
        return "AppUser{id=" + id + ", username='" + username + "', password=***, email='" + email
                + "', role=" + role + "}";
    }
}
