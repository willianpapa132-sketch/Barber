package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao.FinalizacaoAgendamentoResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao.FinalizacaoAgendamentoResponse.ComissaoResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao.FinalizacaoAgendamentoResponse.ComissaoPlanoResponse;
import bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao.FinalizacaoAgendamentoResponse.ServicoResponse;
import bruninho.Barbeiro.config.AppProperties;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;

@Service
public class FinalizacaoAgendamentoService {
    private static final List<FormaPagamento> PAGAMENTOS_AVULSOS = List.of(
            FormaPagamento.DINHEIRO, FormaPagamento.PIX, FormaPagamento.DEBITO, FormaPagamento.CREDITO);
    private final AgendamentoRepository agendamentos;
    private final ComissaoBarbeiroRepository comissoes;
    private final ComissaoPlanoRepository comissoesPlano;
    private final PlanoMensalClienteRepository planos;
    private final Clock clock;

    @Autowired
    public FinalizacaoAgendamentoService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                                         ComissaoPlanoRepository comissoesPlano,
                                         PlanoMensalClienteRepository planos, AppProperties properties) {
        this(agendamentos, comissoes, comissoesPlano, planos, Clock.system(ZoneId.of(properties.getZone())));
    }

    FinalizacaoAgendamentoService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                                  ComissaoPlanoRepository comissoesPlano, PlanoMensalClienteRepository planos, Clock clock) {
        this.agendamentos = agendamentos;
        this.comissoes = comissoes;
        this.comissoesPlano = comissoesPlano;
        this.planos = planos;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FinalizacaoAgendamentoResponse consultar(String login, Long id, FormaPagamento formaPagamento) {
        var agendamento = agendamentos.findByIdAndBarbeiro_Usuario_Login(id, login)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));
        validarEstado(agendamento);
        var calculo = preparar(agendamento, formaPagamento);
        return resposta(agendamento, calculo, LocalDateTime.now(clock));
    }

    @Transactional
    public FinalizacaoAgendamentoResponse finalizar(String login, Long id, FormaPagamento formaPagamento) {
        if (formaPagamento == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a forma de pagamento");
        }
        var agendamento = agendamentos.findParaFinalizar(id, login)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));
        validarEstado(agendamento);
        var agora = LocalDateTime.now(clock);
        if (!agora.isAfter(termino(agendamento))) {
            throw erro(HttpStatus.CONFLICT, "Agendamento so pode ser finalizado apos o horario de termino");
        }
        if (comissoes.existsByAgendamento_Id(id) || comissoesPlano.existsByAgendamento_Id(id)) {
            throw erro(HttpStatus.CONFLICT, "Agendamento ja possui comissao");
        }
        var calculo = preparar(agendamento, formaPagamento);
        if (calculo.plano() != null) {
            var registro = new ComissaoPlano();
            registro.setAgendamento(agendamento);
            registro.setPlanoMensal(calculo.plano().getPlanoMensal());
            registro.setPlanoMensalCliente(calculo.plano());
            registro.setBarbeiro(agendamento.getBarbeiro());
            registro.setDataAgendamento(agendamento.getData());
            registro.setPercentualAplicado(calculo.comissaoPlano().percentualAplicado());
            registro.setCriadoEm(agora);
            comissoesPlano.save(registro);
        } else {
            registrarComissaoPadrao(agendamento, calculo, agora);
        }

        agendamento.setPlanoMensalCliente(calculo.plano());
        agendamento.setFormaPagamento(calculo.formaPagamento());
        agendamento.setPagamentoRecebido(true);
        agendamento.setFinalizadoEm(agora);
        agendamento.setAtualizadoEm(agora);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        return resposta(agendamento, calculo, agora);
    }

    private void registrarComissaoPadrao(Agendamento agendamento, Calculo calculo, LocalDateTime agora) {
        var comissao = new ComissaoBarbeiro();
        comissao.setAgendamento(agendamento);
        comissao.setBarbeiro(agendamento.getBarbeiro());
        comissao.setTipo(calculo.comissao().tipo());
        comissao.setValorBase(calculo.comissao().valorBase());
        comissao.setPercentualAplicado(calculo.comissao().percentualAplicado());
        comissao.setValorComissao(calculo.comissao().valorComissao());
        comissao.setCriadoEm(agora);
        comissoes.save(comissao);
    }

    private Calculo preparar(Agendamento agendamento, FormaPagamento escolha) {
        boolean mensal = agendamento.getPlanoMensalCliente() != null || escolha == FormaPagamento.MENSAL;
        if (agendamento.getPlanoMensalCliente() != null && escolha != null && escolha != FormaPagamento.MENSAL) {
            throw erro(HttpStatus.BAD_REQUEST, "Agendamento do plano mensal deve usar pagamento MENSAL");
        }
        PlanoMensalCliente plano = mensal ? validarPlano(agendamento) : null;
        var servicos = agendamento.getServico().stream()
                .sorted(Comparator.comparing(Servico::getNome).thenComparing(Servico::getId))
                .map(s -> new ServicoResponse(s.getId(), s.getNome(), valorValido(s.getPreco())))
                .toList();
        var valorServicos = servicos.stream().map(ServicoResponse::preco).reduce(BigDecimal.ZERO, BigDecimal::add);
        var comissao = mensal ? null : calcularComissao(agendamento);
        ComissaoPlanoResponse comissaoPlano = null;
        if (mensal) {
            var percentual = percentualValido(plano.getPlanoMensal().getPorcentagemComissao());
            comissaoPlano = new ComissaoPlanoResponse(plano.getPlanoMensal().getId(), plano.getId(),
                    plano.getPlanoMensal().getNomePlano(), agendamento.getData(), percentual);
        }
        return new Calculo(plano, mensal ? FormaPagamento.MENSAL : escolha, servicos, valorServicos,
                mensal ? BigDecimal.ZERO.setScale(2) : valorValido(agendamento.getPrecoTotal()), comissao, comissaoPlano);
    }

    private PlanoMensalCliente validarPlano(Agendamento agendamento) {
        var cliente = agendamento.getCliente();
        if (cliente == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Pagamento mensal exige cliente cadastrado com plano ativo");
        }
        var plano = agendamento.getPlanoMensalCliente();
        if (plano == null) {
            plano = planos.findByCliente_IdAndAtivoTrue(cliente.getId())
                    .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST, "Cliente nao possui plano mensal ativo"));
        }
        if (!Objects.equals(cliente.getId(), plano.getCliente().getId())
                || !Boolean.TRUE.equals(plano.getAtivo()) || plano.getPlanoMensal() == null
                || !Boolean.TRUE.equals(plano.getPlanoMensal().getAtivo())
                || Boolean.TRUE.equals(plano.getPagamentoAtrasado())) {
            throw erro(HttpStatus.BAD_REQUEST, "Plano mensal do cliente inativo, invalido ou com pagamento atrasado");
        }
        var incluidos = plano.getPlanoMensal().getServicosIncluidos().stream().map(Servico::getId).toList();
        if (agendamento.getServico().isEmpty()
                || agendamento.getServico().stream().anyMatch(s -> !incluidos.contains(s.getId()))) {
            throw erro(HttpStatus.BAD_REQUEST, "Servicos do atendimento nao estao incluidos no plano mensal");
        }
        return plano;
    }

    private BigDecimal percentualValido(Integer percentual) {
        if (percentual == null || percentual < 0 || percentual > 100) {
            throw erro(HttpStatus.CONFLICT, "Percentual de comissao deve estar entre 0 e 100");
        }
        return BigDecimal.valueOf(percentual);
    }

    private ComissaoResponse calcularComissao(Agendamento agendamento) {
        var base = valorValido(agendamento.getPrecoTotal());
        var porcentagem = percentualValido(agendamento.getBarbeiro().getPorcentagemComissao());
        var valor = base.multiply(porcentagem).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return new ComissaoResponse(TipoComissao.PADRAO_BARBEIRO, base, porcentagem, valor);
    }

    private void validarEstado(Agendamento agendamento) {
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw erro(HttpStatus.CONFLICT, "Somente agendamentos com status AGENDADO podem ser finalizados");
        }
    }

    private LocalDateTime termino(Agendamento agendamento) {
        if (agendamento.getData() == null || agendamento.getHoraFinalizacao() == null
                || agendamento.getHoraInicio() == null) {
            throw erro(HttpStatus.CONFLICT, "Agendamento sem data ou horario de termino valido");
        }
        var termino = agendamento.getData().atTime(agendamento.getHoraFinalizacao());
        return agendamento.getHoraFinalizacao().isBefore(agendamento.getHoraInicio()) ? termino.plusDays(1) : termino;
    }

    private FinalizacaoAgendamentoResponse resposta(Agendamento agendamento, Calculo calculo, LocalDateTime agora) {
        String nome = agendamento.getNomeCliente();
        if ((nome == null || nome.isBlank()) && agendamento.getCliente() != null) {
            nome = agendamento.getCliente().getNome();
        }
        boolean podeFinalizar = agendamento.getStatus() == StatusAgendamento.AGENDADO && agora.isAfter(termino(agendamento));
        var pagamentos = formasDisponiveis(agendamento, calculo);
        return new FinalizacaoAgendamentoResponse(
                agendamento.getId(), nome, agendamento.getData(), agendamento.getHoraInicio(),
                agendamento.getHoraFinalizacao(), agendamento.getDuracaoServicoMinutos(), agendamento.getStatus(),
                calculo.plano() != null, calculo.servicos(), calculo.valorServicos(), calculo.valorCobrar(),
                calculo.formaPagamento(), pagamentos, calculo.comissao(), calculo.comissaoPlano(), podeFinalizar,
                podeFinalizar ? null : agendamento.getStatus() == StatusAgendamento.CONCLUIDO
                        ? "Atendimento ja finalizado" : "Aguarde o horario de termino do agendamento",
                agendamento.getFinalizadoEm());
    }

    private List<FormaPagamento> formasDisponiveis(Agendamento agendamento, Calculo calculo) {
        if (calculo.plano() != null) {
            return List.of(FormaPagamento.MENSAL);
        }
        try {
            validarPlano(agendamento);
            var formas = new ArrayList<>(PAGAMENTOS_AVULSOS);
            formas.add(FormaPagamento.MENSAL);
            return List.copyOf(formas);
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.BAD_REQUEST) {
                throw exception;
            }
            return PAGAMENTOS_AVULSOS;
        }
    }

    private BigDecimal valorValido(BigDecimal valor) {
        if (valor == null || valor.signum() < 0) {
            throw erro(HttpStatus.CONFLICT, "Valor do atendimento ou servico invalido");
        }
        return valor;
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }

    private record Calculo(PlanoMensalCliente plano, FormaPagamento formaPagamento,
                           List<ServicoResponse> servicos, BigDecimal valorServicos,
                           BigDecimal valorCobrar, ComissaoResponse comissao, ComissaoPlanoResponse comissaoPlano) {}
}
