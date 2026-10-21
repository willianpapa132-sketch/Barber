package bruninho.Barbeiro.Controller.Admin.deshboard;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class DashboardPageController {
    @GetMapping("/admin/dashboard")
    public String pagina(){return "forward:/client/index.html";}
}
