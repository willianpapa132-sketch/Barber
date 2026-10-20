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
    private final CaixaService caixa;
    private CobrancaPlanoService cobranca;

    @Autowired
    public FinalizacaoAgendamentoService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                                         ComissaoPlanoRepository comissoesPlano,
                                         PlanoMensalClienteRepository planos, AppProperties properties, CaixaService caixa,
                                         CobrancaPlanoService cobranca) {
        this(agendamentos, comissoes, comissoesPlano, planos, Clock.system(ZoneId.of(properties.getZone())), caixa);
        this.cobranca = cobranca;
    }

    FinalizacaoAgendamentoService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                                  ComissaoPlanoRepository comissoesPlano, PlanoMensalClienteRepository planos, Clock clock, CaixaService caixa) {
        this.agendamentos = agendamentos;
        this.comissoes = comissoes;
        this.comissoesPlano = comissoesPlano;
        this.planos = planos;
        this.clock = clock;
        this.caixa = caixa;
    }

    // Mantem a construcao usada pelos testes unitarios existentes.
    FinalizacaoAgendamentoService(AgendamentoRepository agendamentos, ComissaoBarbeiroRepository comissoes,
                                  ComissaoPlanoRepository comissoesPlano, PlanoMensalClienteRepository planos, Clock clock) {
        this(agendamentos, comissoes, comissoesPlano, planos, clock, null);
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
        if (calculo.comissaoPlano() != null && calculo.comissaoPlano().pontos() > 0) {
            var registro = new ComissaoPlano();
            registro.setAgendamento(agendamento);
            registro.setPlanoMensal(calculo.plano().getPlanoMensal());
            registro.setPlanoMensalCliente(calculo.plano());
            registro.setBarbeiro(agendamento.getBarbeiro());
            registro.setDataAgendamento(agendamento.getData());
            registro.setPontos(calculo.comissaoPlano().pontos());
            registro.setCriadoEm(agora);
            comissoesPlano.save(registro);
        }
        if (calculo.comissao() != null) {
            registrarComissaoPadrao(agendamento, calculo, agora);
        }
        if (calculo.plano() != null && agendamento.getServicosPlano().isEmpty()) {
            agendamento.getServicosPlano().addAll(CalculoServicosPlano.calcular(agendamento.getServico(), calculo.plano().getPlanoMensal()));
        }
        agendamento.setPrecoTotal(calculo.valorCobrar());

        agendamento.setPlanoMensalCliente(calculo.plano());
        agendamento.setFormaPagamento(calculo.formaPagamento());
        agendamento.setPagamentoRecebido(true);
        agendamento.setFinalizadoEm(agora);
        agendamento.setAtualizadoEm(agora);
        agendamento.setStatus(StatusAgendamento.CONCLUIDO);
        if (caixa != null) caixa.registrarEntrada(agendamento, calculo.valorCobrar(), agora);
        return resposta(agendamento, calculo, agora);
    }

    @Transactional
    public void cancelar(String login, Long id) {
        alterarStatus(login, id, StatusAgendamento.CANCELADO);
    }

    @Transactional
    public void registrarNaoComparecimento(String login, Long id) {
        alterarStatus(login, id, StatusAgendamento.NAO_COMPARECEU);
    }

    private void alterarStatus(String login, Long id, StatusAgendamento novoStatus) {
        var agendamento = agendamentos.findParaFinalizar(id, login)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Agendamento nao encontrado"));
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw erro(HttpStatus.CONFLICT, "Somente agendamentos com status AGENDADO podem ser alterados");
        }
        if (comissoes.existsByAgendamento_Id(id) || comissoesPlano.existsByAgendamento_Id(id)
                || Boolean.TRUE.equals(agendamento.getPagamentoRecebido())) {
            throw erro(HttpStatus.CONFLICT, "Agendamento ja possui pagamento ou comissao");
        }
        var agora = LocalDateTime.now(clock);
        if (novoStatus == StatusAgendamento.NAO_COMPARECEU
                && (agendamento.getData() == null || agendamento.getHoraInicio() == null
                || agora.isBefore(agendamento.getData().atTime(agendamento.getHoraInicio())))) {
            throw erro(HttpStatus.CONFLICT, "Nao comparecimento so pode ser registrado a partir do horario de inicio");
        }
        agendamento.setStatus(novoStatus);
        agendamento.setAtualizadoEm(agora);
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
        PlanoMensalCliente plano = mensal ? validarPlano(agendamento) : null;
        var itens = plano != null && !agendamento.getServicosPlano().isEmpty()
                ? agendamento.getServicosPlano()
                : CalculoServicosPlano.calcular(agendamento.getServico(), plano == null ? null : plano.getPlanoMensal());
        var servicos = itens.stream().map(i -> new ServicoResponse(i.getServicoId(), i.getNome(),
                i.getPrecoOriginal(), i.getPrecoLiquido(), i.getPorcentagemDesconto(), i.isInclusoNoPlano())).toList();
        var valorServicos = itens.stream().map(ServicoAgendadoPlano::getPrecoOriginal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var cobrar = plano == null ? valorValido(agendamento.getPrecoTotal()) : CalculoServicosPlano.total(itens);
        boolean temExtras = plano != null && itens.stream().anyMatch(i -> !i.isInclusoNoPlano());
        if (plano != null && !temExtras && escolha != null && escolha != FormaPagamento.MENSAL) {
            throw erro(HttpStatus.BAD_REQUEST, "Agendamento do plano mensal deve usar pagamento MENSAL");
        }
        if (temExtras && cobrar.signum() > 0 && escolha == FormaPagamento.MENSAL) {
            throw erro(HttpStatus.BAD_REQUEST, "Selecione a forma de pagamento dos servicos extras");
        }
        var comissao = plano == null || (temExtras && cobrar.signum() > 0) ? calcularComissao(agendamento, cobrar) : null;
        ComissaoPlanoResponse comissaoPlano = null;
        if (plano != null) {
            int pontos = CalculoServicosPlano.pontos(itens);
            // Agendamentos anteriores ao detalhamento por servico eram integralmente cobertos.
            if (agendamento.getServicosPlano().isEmpty() && !temExtras) {
                pontos = agendamento.getDuracaoServicoMinutos();
                if (pontos <= 0) throw erro(HttpStatus.CONFLICT, "Agendamento sem duracao valida para gerar pontos");
            }
            comissaoPlano = new ComissaoPlanoResponse(plano.getPlanoMensal().getId(), plano.getId(),
                    plano.getPlanoMensal().getNomePlano(), agendamento.getData(), pontos);
        }
        var pagamento = plano != null && cobrar.signum() == 0 ? FormaPagamento.MENSAL : escolha;
        return new Calculo(plano, pagamento, servicos, valorServicos, cobrar, comissao, comissaoPlano);
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
                || (cobranca != null ? cobranca.bloqueado(plano) : Boolean.TRUE.equals(plano.getPagamentoAtrasado()))) {
            throw erro(HttpStatus.BAD_REQUEST, "Plano mensal do cliente inativo, invalido ou com pagamento atrasado");
        }
        if (agendamento.getServico().isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Selecione os servicos do atendimento");
        }
        return plano;
    }

    private BigDecimal percentualValido(Integer percentual) {
        if (percentual == null || percentual < 0 || percentual > 100) {
            throw erro(HttpStatus.CONFLICT, "Percentual de comissao deve estar entre 0 e 100");
        }
        return BigDecimal.valueOf(percentual);
    }

    private ComissaoResponse calcularComissao(Agendamento agendamento, BigDecimal base) {
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
            return calculo.valorCobrar().signum() > 0 ? PAGAMENTOS_AVULSOS : List.of(FormaPagamento.MENSAL);
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
