package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.comissao.ComissaoAdminResponse;
import bruninho.Barbeiro.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class ComissaoAdminService {
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;
    private final ComissaoPlanoRepository pontos;
    private final ComissaoBarbeiroRepository comissoes;
    public ComissaoAdminService(BarbeiroRepository barbeiros, AgendamentoRepository agendamentos,
            ComissaoPlanoRepository pontos, ComissaoBarbeiroRepository comissoes) {
        this.barbeiros = barbeiros; this.agendamentos = agendamentos; this.pontos = pontos; this.comissoes = comissoes;
    }
    @Transactional(readOnly = true)
    public ComissaoAdminResponse consultar(LocalDate inicio, LocalDate fim, Long id) {
        var hoje = LocalDate.now();
        if(inicio == null && fim == null) { inicio = hoje.withDayOfMonth(1); fim = hoje; }
        CarteiraBarbeiroService.validarPeriodo(inicio, fim);
        if(id != null && !barbeiros.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado");
        final var de = inicio; final var ate = fim;
        var lista = barbeiros.findAll(Sort.by("nome", "id")).stream().map(b -> new ComissaoAdminResponse.Resumo(
                b.getId(), b.getNome(), b.isAtivo(), agendamentos.somarMinutosConcluidos(b.getId(), de, ate),
                pontos.somarPontosConcluidos(b.getId(), de, ate),
                comissoes.somarComissoesConcluidas(b.getId(), de, ate).setScale(2))).toList();
        var selecionados = lista.stream().filter(b -> id == null || id.equals(b.id())).toList();
        var dia = selecionados.stream().map(b -> comissoes.somarComissoesConcluidas(b.id(), hoje, hoje))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new ComissaoAdminResponse(inicio, fim, hoje, id,
                selecionados.stream().mapToLong(ComissaoAdminResponse.Resumo::minutosTrabalhados).sum(),
                selecionados.stream().mapToLong(ComissaoAdminResponse.Resumo::pontosPlano).sum(),
                selecionados.stream().map(ComissaoAdminResponse.Resumo::valorComissoes).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2), dia, lista);
    }
}
