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
            return new AtendimentoFormuladoResponse(a.getId(), c.getComanda().getFechadoEm().toLocalDate(), a.getHoraInicio(), a.getHoraFinalizacao(),
                    c.getComanda().getMinutos(), c.getComanda().getNomeCliente(), itens(c.getComanda(), true), c.getPlanoMensal().getNomePlano(),
                    c.getPontos(), null, null, null, c.getComanda().getId(), c.getComanda().getFechadoEm(), List.copyOf(c.getComanda().getItens()));
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
            return new AtendimentoFormuladoResponse(a.getId(), c.getComanda().getFechadoEm().toLocalDate(), a.getHoraInicio(), a.getHoraFinalizacao(),
                    c.getComanda().getMinutos(), c.getComanda().getNomeCliente(), itens(c.getComanda(), false),
                    a.getPlanoMensalCliente() == null ? null : a.getPlanoMensalCliente().getPlanoMensal().getNomePlano(),
                    null, c.getValorBase(), c.getPercentualAplicado(), c.getValorComissao(), c.getComanda().getId(), c.getComanda().getFechadoEm(), List.copyOf(c.getComanda().getItens()));
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

    private List<String> itens(Comanda c,boolean incluidos) {
        return c.getItens().stream().filter(i->i.isIncluidoNoPlano()==incluidos)
            .map(i->i.getNome()+(i.getQuantidade()>1?" x"+i.getQuantidade():"")).toList();
    }
}
