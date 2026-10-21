package bruninho.Barbeiro.Controller.Admin.agendamentos;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class AgendamentosAdminPageController {
    @GetMapping("/admin/agendamentos")
    public String pagina() { return "forward:/client/index.html"; }
}
