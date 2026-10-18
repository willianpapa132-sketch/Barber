package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ValoresFormuladoService {
    private static final int TAMANHO_PAGINA = 20;
    private final BarbeiroRepository barbeiros;
    private final ComissaoPlanoRepository pontos;
    private final ComissaoBarbeiroRepository comissoes;

    public ValoresFormuladoService(BarbeiroRepository barbeiros, ComissaoPlanoRepository pontos,
                                   ComissaoBarbeiroRepository comissoes) {
        this.barbeiros = barbeiros; this.pontos = pontos; this.comissoes = comissoes;
    }

    public PontosFormuladoResponse pontos(String login, LocalDate inicio, LocalDate fim, int pagina) {
        Long id = validar(login, inicio, fim, pagina);
        var registros = pontos.extratoConcluidos(id, inicio, fim, PageRequest.of(pagina, TAMANHO_PAGINA));
        validarPagina(registros, pagina);
        var itens = registros.stream().map(c -> {
            var a = c.getAgendamento();
            return new AtendimentoFormuladoResponse(a.getId(), a.getData(), a.getHoraInicio(), a.getHoraFinalizacao(),
                    a.getDuracaoServicoMinutos(), nomeCliente(a), servicos(a, true), c.getPlanoMensal().getNomePlano(),
                    c.getPontos(), null, null, null);
        }).toList();
        return new PontosFormuladoResponse(inicio, fim, pontos.somarPontosConcluidos(id, inicio, fim),
                registros.getTotalElements(), pagina, registros.getTotalPages(), itens);
    }

    public ComissoesFormuladoResponse comissoes(String login, LocalDate inicio, LocalDate fim, int pagina) {
        Long id = validar(login, inicio, fim, pagina);
        var registros = comissoes.extratoConcluidos(id, inicio, fim, PageRequest.of(pagina, TAMANHO_PAGINA));
        validarPagina(registros, pagina);
        var itens = registros.stream().map(c -> {
            var a = c.getAgendamento();
            return new AtendimentoFormuladoResponse(a.getId(), a.getData(), a.getHoraInicio(), a.getHoraFinalizacao(),
                    a.getDuracaoServicoMinutos(), nomeCliente(a), servicos(a, false),
                    a.getPlanoMensalCliente() == null ? null : a.getPlanoMensalCliente().getPlanoMensal().getNomePlano(),
                    null, c.getValorBase(), c.getPercentualAplicado(), c.getValorComissao());
        }).toList();
        return new ComissoesFormuladoResponse(inicio, fim, comissoes.somarComissoesConcluidas(id, inicio, fim).setScale(2),
                registros.getTotalElements(), pagina, registros.getTotalPages(), itens);
    }

    private Long validar(String login, LocalDate inicio, LocalDate fim, int pagina) {
        CarteiraBarbeiroService.validarPeriodo(inicio, fim);
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina invalida");
        return barbeiros.findByUsuario_Login(login).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado")).getId();
    }

    private void validarPagina(Page<?> registros, int pagina) {
        if (pagina > 0 && pagina >= registros.getTotalPages()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pagina fora do resultado");
        }
    }

    private String nomeCliente(Agendamento a) {
        if (a.getNomeCliente() != null && !a.getNomeCliente().isBlank()) return a.getNomeCliente();
        return a.getCliente() != null ? a.getCliente().getNome() : "Cliente avulso";
    }

    private List<String> servicos(Agendamento a, boolean incluidos) {
        if (!a.getServicosPlano().isEmpty()) {
            return a.getServicosPlano().stream().filter(s -> s.isInclusoNoPlano() == incluidos)
                    .map(ServicoAgendadoPlano::getNome).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        }
        // Registros antigos nao possuem detalhamento: os pontos e valores salvos continuam sendo a referencia.
        return a.getServico().stream().sorted(Comparator.comparing(Servico::getNome, String.CASE_INSENSITIVE_ORDER))
                .map(Servico::getNome).toList();
    }
}
