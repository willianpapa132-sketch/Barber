package bruninho.Barbeiro.Controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/home";
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (usuarioAutenticado(authentication)) {
            return "redirect:/home";
        }
        return "login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication) {
        boolean barbeiro = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_BARBEIRO"));

        if (barbeiro) {
            return "redirect:/barbeiro/home";
        }
        return "home";
    }

    private boolean usuarioAutenticado(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
