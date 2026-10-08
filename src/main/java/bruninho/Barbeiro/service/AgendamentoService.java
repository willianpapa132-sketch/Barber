package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.request.CriarAgendamentoRequest;
import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgendamentoService {
    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;
    private final PlanoMensalClienteRepository planosCliente;
    private final HorarioFuncionamentoRepository horariosFuncionamento;
    private final ValidacaoAgendamentoService validacao;
    private final DisponibilidadeAgendamentosService disponibilidade;

    public AgendamentoService(
            AgendamentoRepository agendamentos,
            BarbeiroRepository barbeiros,
            PlanoMensalClienteRepository planosCliente,
            HorarioFuncionamentoRepository horariosFuncionamento,
            ValidacaoAgendamentoService validacao,
            DisponibilidadeAgendamentosService disponibilidade
    ) {
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.planosCliente = planosCliente;
        this.horariosFuncionamento = horariosFuncionamento;
        this.validacao = validacao;
        this.disponibilidade = disponibilidade;
    }

    @Transactional
    public Agendamento agendar(CriarAgendamentoRequest request) {
        var cliente = validacao.validarCliente(request.usuarioId(), request.clienteId());
        var planoCliente = planosCliente.findByCliente_IdAndAtivoTrue(cliente.getId());

        if (planoCliente.isPresent()) {
            return agendarComPlano(request, cliente, planoCliente.get());
        }

        return agendarNormal(request, cliente);
    }

    @Transactional
    public Agendamento agendarComPlano(CriarAgendamentoRequest request) {
        var cliente = validacao.validarCliente(request.usuarioId(), request.clienteId());
        var planoCliente = planosCliente.findByCliente_IdAndAtivoTrue(cliente.getId())
                .orElseThrow(() -> new BusinessException("Cliente nao possui plano mensal ativo"));

        return agendarComPlano(request, cliente, planoCliente);
    }

    private Agendamento agendarNormal(CriarAgendamentoRequest request, Cliente cliente) {
        validacao.validarId(request.barbeiroId());
        var barbeiro = barbeiros.findByIdParaAgendar(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro nao encontrado"));
        validacao.validarBarbeiroAtivo(barbeiro);

        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(validacao.validarServicos(request.servicosIds()));
        agendamento.setData(request.data());
        agendamento.setHoraInicio(request.horaInicio());
        validacao.validaAgendamento(agendamento, disponibilidade);
        preencherDadosDoAgendamento(agendamento, cliente, agendamento.getServico().stream().map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return agendamentos.save(agendamento);
    }

    private Agendamento agendarComPlano(
            CriarAgendamentoRequest request,
            Cliente cliente,
            PlanoMensalCliente planoCliente
    ) {
        validarPlanoDoCliente(planoCliente);

        validacao.validarId(request.barbeiroId());
        var barbeiro = barbeiros.findByIdParaAgendar(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro nao encontrado"));
        validacao.validarBarbeiroAtivo(barbeiro);

        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(validacao.validarServicos(request.servicosIds()));
        agendamento.setPlanoMensalCliente(planoCliente);
        agendamento.setData(request.data());
        agendamento.setHoraInicio(request.horaInicio());

        validarServicosDentroDoPlano(agendamento.getServico(), planoCliente);
        validarDiaPermitidoParaPlano(request.data());
        validarLimiteSemanalDoPlano(planoCliente, request.data());
        validacao.validaAgendamento(agendamento, disponibilidade);
        preencherDadosDoAgendamento(agendamento, cliente, BigDecimal.ZERO);

        return agendamentos.save(agendamento);
    }

    private void validarPlanoDoCliente(PlanoMensalCliente planoCliente) {
        if (!Boolean.TRUE.equals(planoCliente.getAtivo()) || !Boolean.TRUE.equals(planoCliente.getPlanoMensal().getAtivo())) {
            throw new BusinessException("Plano mensal inativo");
        }
    }

    private void validarServicosDentroDoPlano(Set<Servico> servicosSolicitados, PlanoMensalCliente planoCliente) {
        Set<Long> servicosDoPlano = planoCliente.getPlanoMensal().getServicosIncluidos().stream()
                .map(Servico::getId)
                .collect(Collectors.toSet());

        boolean todosIncluidos = servicosSolicitados.stream()
                .map(Servico::getId)
                .allMatch(servicosDoPlano::contains);

        if (!todosIncluidos) {
            throw new BusinessException("Servico solicitado nao pertence ao plano mensal do cliente");
        }
    }

    private void validarDiaPermitidoParaPlano(LocalDate data) {
        var horario = horariosFuncionamento.findByDiaSemana(data.getDayOfWeek());
        if (horario == null || !Boolean.TRUE.equals(horario.getPermiteAgendamentoPlano())) {
            throw new BusinessException("Plano mensal nao permite agendamento neste dia da semana");
        }
    }

    private void validarLimiteSemanalDoPlano(PlanoMensalCliente planoCliente, LocalDate data) {
        LocalDate inicioSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate fimSemana = data.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        boolean jaPossuiAgendamentoNaSemana = agendamentos.existsByPlanoMensalCliente_IdAndDataBetweenAndStatusNotIn(
                planoCliente.getId(),
                inicioSemana,
                fimSemana,
                List.of(StatusAgendamento.CANCELADO, StatusAgendamento.NAO_COMPARECEU)
        );

        if (jaPossuiAgendamentoNaSemana) {
            throw new BusinessException("Plano mensal permite apenas um agendamento por semana");
        }
    }

    private void preencherDadosDoAgendamento(Agendamento agendamento, Cliente cliente, BigDecimal precoTotal) {
        int minutos = agendamento.getServico().stream().mapToInt(Servico::getDuracaoMinutos).sum();
        agendamento.setDuracaoServicoMinutos(minutos);
        agendamento.setHoraFinalizacao(agendamento.getHoraInicio().plusMinutes(minutos));
        agendamento.setPrecoTotal(precoTotal);
        agendamento.setNomeCliente(cliente.getNome());
        agendamento.setTelefoneCliente(cliente.getTelefone());
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setPagamentoRecebido(false);
        agendamento.setCriadoEm(LocalDateTime.now());
    }
}
