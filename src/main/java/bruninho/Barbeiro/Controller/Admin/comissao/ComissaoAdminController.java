package bruninho.Barbeiro.Controller.Admin.comissao;
import bruninho.Barbeiro.service.ComissaoAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/admin/comissao")
public class ComissaoAdminController {
    private final ComissaoAdminService service;
    public ComissaoAdminController(ComissaoAdminService service) { this.service = service; }
    @GetMapping
    public ComissaoAdminResponse consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) Long barbeiroId) {
        return service.consultar(inicio, fim, barbeiroId);
    }
}
