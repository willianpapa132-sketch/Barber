package bruninho.Barbeiro.web;

import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioControlador {
    private final ConfiguracaoBarbeariaRepositorio configuracoes;
    public InicioControlador(ConfiguracaoBarbeariaRepositorio configuracoes) { this.configuracoes = configuracoes; }

    @GetMapping("/")
    String home(org.springframework.ui.Model model) {
        model.addAttribute("config", configuracoes.findById(1L).orElseThrow());
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
