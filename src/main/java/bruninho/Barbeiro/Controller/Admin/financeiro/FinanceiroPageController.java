package bruninho.Barbeiro.Controller.Admin.financeiro;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FinanceiroPageController {
    @GetMapping("/admin/financeiro")
    public String pagina() { return "forward:/client/index.html"; }
}
