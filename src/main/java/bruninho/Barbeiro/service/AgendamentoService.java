package bruninho.Barbeiro.service;

import bruninho.Barbeiro.exception.NotFoundException;

import bruninho.Barbeiro.Controller.cliente.request.CriarAgendamentoRequest;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class AgendamentoService {
    private final AgendamentoRepository agendamentos;
    private final BarbeiroRepository barbeiros;
    private final ValidacaoAgendamentoService validacao;
    private final DisponibilidadeAgendamentosService disponibilidade;

    public AgendamentoService(AgendamentoRepository agendamentos, BarbeiroRepository barbeiros,
            ValidacaoAgendamentoService validacao, DisponibilidadeAgendamentosService disponibilidade) {
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.validacao = validacao;
        this.disponibilidade = disponibilidade;
    }

    @Transactional
    public Agendamento agendar(CriarAgendamentoRequest request) {
        var cliente = validacao.validarCliente(request.usuarioId(), request.clienteId());
        validacao.validarId(request.barbeiroId());
        // Serializa criações para o mesmo barbeiro até o commit.
        var barbeiro = barbeiros.findByIdParaAgendar(request.barbeiroId())
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));
        validacao.validarBarbeiroAtivo(barbeiro);
        var agendamento = new Agendamento();
        agendamento.setCliente(cliente);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(validacao.validarServicos(request.servicosIds()));
        agendamento.setData(request.data());
        agendamento.setHoraInicio(request.horaInicio());
        validacao.validaAgendamento(agendamento, disponibilidade);
        int minutos = agendamento.getServico().stream().mapToInt(Servico::getDuracaoMinutos).sum();
        agendamento.setDuracaoServicoMinutos(minutos);
        agendamento.setHoraFinalizacao(request.horaInicio().plusMinutes(minutos));
        agendamento.setPrecoTotal(agendamento.getServico().stream().map(Servico::getPreco)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        agendamento.setNomeCliente(cliente.getNome());
        agendamento.setTelefoneCliente(cliente.getTelefone());
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setPagamentoRecebido(false);
        agendamento.setCriadoEm(LocalDateTime.now());
        return agendamentos.save(agendamento);
    }
}
