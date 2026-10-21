package bruninho.Barbeiro.Controller.Admin.comissao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class ComissaoPageController {
    @GetMapping("/admin/comissao")
    public String pagina() { return "forward:/client/index.html"; }
}
