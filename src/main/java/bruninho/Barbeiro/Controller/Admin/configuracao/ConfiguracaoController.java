package bruninho.Barbeiro.Controller.Admin.configuracao;
import bruninho.Barbeiro.service.HorariosService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/admin/configuracao")
public class ConfiguracaoController {
    private final HorariosService service;
    public ConfiguracaoController(HorariosService service){this.service=service;}
    @GetMapping public ConfiguracaoResponse consultar(){return service.consultar();}
    @PutMapping public ConfiguracaoResponse salvar(@Valid @RequestBody SalvarConfiguracaoRequest r){return service.salvar(r);}
}
