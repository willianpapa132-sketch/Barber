package bruninho.Barber.web;

import bruninho.Barber.repository.BarberShopConfigRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {
    private final BarberShopConfigRepository configs;
    public RootController(BarberShopConfigRepository configs) { this.configs = configs; }

    @GetMapping("/")
    String home(org.springframework.ui.Model model) {
        model.addAttribute("config", configs.findById(1L).orElseThrow());
        return "public/home";
    }

    @GetMapping("/login")
    String login() { return "login"; }

    @GetMapping("/pos-login")
    String afterLogin(Authentication auth) {
        boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return admin ? "redirect:/admin" : "redirect:/barbeiro/agenda";
    }
}
