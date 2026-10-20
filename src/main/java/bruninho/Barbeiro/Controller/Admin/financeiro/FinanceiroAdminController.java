package bruninho.Barbeiro.Controller.Admin.financeiro;

import bruninho.Barbeiro.config.AppProperties;
import bruninho.Barbeiro.service.FinanceiroService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/admin/financeiro")
public class FinanceiroAdminController {
    private final FinanceiroService service;
    private final ZoneId zona;
    public FinanceiroAdminController(FinanceiroService service, AppProperties properties) {
        this.service = service;
        this.zona = ZoneId.of(properties.getZone());
    }
    @GetMapping
    public FinanceiroResponse consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        var hoje = LocalDate.now(zona);
        return service.consultar(inicio == null ? hoje.withDayOfMonth(1) : inicio, fim == null ? hoje : fim);
    }
}
