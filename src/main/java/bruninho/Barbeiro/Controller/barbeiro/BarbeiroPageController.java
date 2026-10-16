package bruninho.Barbeiro.Controller.barbeiro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/barbeiro")
public class BarbeiroPageController {
    @GetMapping({"", "/home", "/agenda"})
    public String pagina() {
        return "forward:/client/index.html";
    }
}
