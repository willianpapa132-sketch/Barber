package bruninho.Barbeiro.Controller.Admin.configuracao;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class ConfiguracaoPageController {
    @GetMapping("/admin/configuracao") public String pagina(){return "forward:/client/index.html";}
}
