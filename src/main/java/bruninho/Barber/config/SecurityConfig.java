package bruninho.Barber.config;

import bruninho.Barber.domain.AppUser;
import bruninho.Barber.repository.AppUserRepository;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/agendar/**", "/css/**", "/js/**", "/login").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/barbeiro/**").hasRole("BARBEIRO")
                .anyRequest().authenticated())
            .formLogin(login -> login
                .loginPage("/login")
                .defaultSuccessUrl("/pos-login", true)
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID"))
            .sessionManagement(session -> session.maximumSessions(1));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(AppUserRepository users) {
        return username -> {
            AppUser user = users.findByUsername(username).orElseThrow();
            if (!user.isActive()) {
                throw new DisabledException("Usuário inativo");
            }
            return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );
        };
    }
}
