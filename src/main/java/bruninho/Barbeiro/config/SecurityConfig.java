package bruninho.Barbeiro.config;

import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import bruninho.Barbeiro.platform.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AppProperties props, ObjectProvider<TenantRequestFilter> tenantFilter) throws Exception {
        if(props.getMultiTenancy().isEnabled())http.addFilterBefore(tenantFilter.getObject(),UsernamePasswordAuthenticationFilter.class);
        return http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(request -> {
                            String path=request.getRequestURI().substring(request.getContextPath().length());
                            return (path.startsWith("/api/")&&!path.startsWith("/api/plataforma/"))||path.equals("/logout");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/login", "/favicon.ico", "/api/auth/**").permitAll()
                        .requestMatchers("/plataforma", "/api/plataforma/**").hasRole("PLATFORM_ADMIN")
                        .requestMatchers("/admin/**", "/api/admin/**").hasRole(ROLE.ADMIN.name())
                        .requestMatchers("/barbeiro/**", "/api/barbeiro/**").hasRole(ROLE.BARBEIRO.name())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(authenticationSuccessHandler(props))
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Login ou senha invalidos.\"}");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(UsuarioRepository usuarios, AppProperties props, ObjectProvider<TenantRegistry> registry) {
        return login -> {
            if(props.getMultiTenancy().isEnabled()) {
                if(TenantContext.PLATFORM.equals(TenantContext.get())) {
                    var passwords=registry.getObject().central().query("select senha from platform_admin where login=?",(rs,n)->rs.getString(1),login.trim().toLowerCase(java.util.Locale.ROOT));
                    if(passwords.isEmpty())throw new UsernameNotFoundException("Usuario nao encontrado");
                    return org.springframework.security.core.userdetails.User.withUsername(login).password(passwords.getFirst()).roles("PLATFORM_ADMIN").build();
                }
                if(TenantContext.get()==null)throw new UsernameNotFoundException("Empresa nao identificada");
            }
            return usuarios.findByLogin(login).orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado"));
        };
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler(AppProperties props) {
        return (request, response, authentication) -> {
            boolean barbeiro = authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + ROLE.BARBEIRO.name()));
            boolean platform=authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
            if(props.getMultiTenancy().isEnabled())request.getSession().setAttribute(TenantContext.SESSION_SCOPE,TenantContext.get());
            String redirectUrl = platform ? "/plataforma" : barbeiro ? "/barbeiro/home" : "/";
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"redirectUrl\":\"" + redirectUrl + "\"}");
        };
    }
}
