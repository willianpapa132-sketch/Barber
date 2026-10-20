package bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class GerenciamentoPlanosPageController {
    @GetMapping("/admin/gerenciamento-planos")
    public String pagina() { return "forward:/client/index.html"; }
}
