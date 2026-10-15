package bruninho.Barbeiro.Controller.barbeiro.home;

import bruninho.Barbeiro.service.HomeBarbeiroService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/barbeiro/home")
public class BarbeiroHomeController {
    private final HomeBarbeiroService homeBarbeiroService;

    public BarbeiroHomeController(HomeBarbeiroService homeBarbeiroService) {
        this.homeBarbeiroService = homeBarbeiroService;
    }

    @GetMapping
    public BarbeiroHomeResponse home(Authentication authentication) {
        return homeBarbeiroService.homeDoBarbeiro(authentication.getName());
    }
}
