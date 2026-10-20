package bruninho.Barbeiro.service;

import bruninho.Barbeiro.config.AppProperties;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepository;
import org.springframework.stereotype.Service;
import java.time.*;

@Service
public class CobrancaPlanoService {
    private final ConfiguracaoBarbeariaRepository configuracoes;
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public CobrancaPlanoService(ConfiguracaoBarbeariaRepository configuracoes, AppProperties properties) {
        this(configuracoes, Clock.system(ZoneId.of(properties.getZone())));
    }
    CobrancaPlanoService(ConfiguracaoBarbeariaRepository configuracoes, Clock clock) {
        this.configuracoes = configuracoes; this.clock = clock;
    }
    public LocalDate hoje() { return LocalDate.now(clock); }
    public LocalDateTime agora() { return LocalDateTime.now(clock); }
    public int tolerancia() {
        return configuracoes.findById(1L).map(c -> c.getDiasToleranciaPagamentoPlano() == null
                ? 0 : c.getDiasToleranciaPagamentoPlano()).orElse(0);
    }
    public LocalDate vencimento(PlanoMensalCliente p) {
        return p.getProximoVencimento() == null ? p.getCriadoEm().toLocalDate() : p.getProximoVencimento();
    }
    public LocalDate noMes(PlanoMensalCliente p, YearMonth mes) {
        return mes.atDay(Math.min(p.getCriadoEm().getDayOfMonth(), mes.lengthOfMonth()));
    }
    public LocalDate proximo(PlanoMensalCliente p) {
        return noMes(p, YearMonth.from(vencimento(p)).plusMonths(1));
    }
    public LocalDate fimLiberacao(PlanoMensalCliente p) {
        var proxima = noMes(p, YearMonth.from(hoje()));
        if (!proxima.isAfter(hoje())) proxima = noMes(p, YearMonth.from(hoje()).plusMonths(1));
        return proxima.minusDays(1);
    }
    public boolean bloqueado(PlanoMensalCliente p) {
        return bloqueado(p, tolerancia());
    }
    public boolean bloqueado(PlanoMensalCliente p, int dias) {
        if (!Boolean.TRUE.equals(p.getAtivo())) return false;
        if (p.getLiberadoAte() != null && !hoje().isAfter(p.getLiberadoAte())) return false;
        return Boolean.TRUE.equals(p.getPagamentoAtrasado()) || hoje().isAfter(vencimento(p).plusDays(dias));
    }
}
