package bruninho.Barbeiro.Controller.Admin.comanda;
@org.springframework.stereotype.Controller
public class ComandaPageController {
    @org.springframework.web.bind.annotation.GetMapping({"/admin/agendamentos/{id}/comanda","/admin/comandas","/admin/comandas/{id}"})
    public String page(){return "forward:/client/index.html";}
}
