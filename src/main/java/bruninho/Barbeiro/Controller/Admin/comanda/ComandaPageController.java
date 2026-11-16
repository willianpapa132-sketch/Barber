package bruninho.Barbeiro.Controller.Admin.comanda;
@org.springframework.stereotype.Controller
public class ComandaPageController {
    @org.springframework.web.bind.annotation.GetMapping("/admin/agendamentos/{id}/comanda")
    public String page(){return "forward:/client/index.html";}
}
