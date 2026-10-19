package bruninho.Barbeiro.Controller.Admin;

import bruninho.Barbeiro.config.AppProperties;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ComissaoBarbeiroRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;

@RestController
@RequestMapping("/api/admin/home")
public class HomeAdminController {
    private final AgendamentoRepository agendamentos;
    private final ComissaoBarbeiroRepository comissoes;
    private final ZoneId zona;

    public HomeAdminController(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes, AppProperties properties) {
        this.agendamentos = agendamentos;
        this.comissoes = comissoes;
        this.zona = ZoneId.of(properties.getZone());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Resumo home() {
        var hoje = LocalDate.now(zona);
        return new Resumo(hoje, agendamentos.countByData(hoje),
                agendamentos.somarValoresConcluidos(hoje).subtract(comissoes.somarComissoesConcluidasNoDia(hoje)));
    }

    public record Resumo(LocalDate data, long agendamentosHoje, BigDecimal valorLiquidoHoje) {}
}
