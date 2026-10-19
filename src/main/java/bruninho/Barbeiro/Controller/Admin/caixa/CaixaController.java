package bruninho.Barbeiro.Controller.Admin.caixa;
import bruninho.Barbeiro.service.CaixaService;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/barbeiro/caixa")
public class CaixaController {
    private final CaixaService service;
    public CaixaController(CaixaService service){this.service=service;}

    @GetMapping public CaixaResponse atual(){
        return CaixaResponse.de(service.atual());
    }
}
