package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento.CriarAtendimentoBarbeiroRequest;
import bruninho.Barbeiro.Controller.barbeiro.agenda.bloqueioeagendamento.CriarBloqueioAgendaRequest;
import bruninho.Barbeiro.Controller.cliente.home.agendamentos.CriarAgendamentoRequest;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class GestaoAgendaBarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final ClienteRepository clientes;
    private final AgendamentoRepository agendamentos;
    private final BloqueioBarbeiroRepository bloqueios;
    private final GradeAgendaBarbeiroService grade;
    private final AgendamentoService criacao;
    private final ValidacaoAgendamentoService validacao;
    private final DisponibilidadeAgendamentosService disponibilidade;

    public GestaoAgendaBarbeiroService(BarbeiroRepository barbeiros, ClienteRepository clientes,
                                      AgendamentoRepository agendamentos, BloqueioBarbeiroRepository bloqueios,
                                      GradeAgendaBarbeiroService grade, AgendamentoService criacao,
                                      ValidacaoAgendamentoService validacao, DisponibilidadeAgendamentosService disponibilidade) {
        this.barbeiros = barbeiros; this.clientes = clientes; this.agendamentos = agendamentos;
        this.bloqueios = bloqueios; this.grade = grade; this.criacao = criacao;
        this.validacao = validacao; this.disponibilidade = disponibilidade;
    }

    public record CriacaoResponse(Long id, String tipo, LocalDate data, LocalTime inicio, LocalTime fim) {}

    @Transactional
    public CriacaoResponse criarAtendimento(String login, CriarAtendimentoBarbeiroRequest request) {
        var barbeiro = bloquearBarbeiro(login);
        Agendamento agendamento;
        if (request.clienteId() != null) {
            var cliente = clientes.findById(request.clienteId())
                    .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));
            if (cliente.getUsuario() == null) throw erro(HttpStatus.BAD_REQUEST, "Cliente sem usuario cadastrado");
            agendamento = criacao.agendar(new CriarAgendamentoRequest(cliente.getUsuario().getId(), cliente.getId(),
                    barbeiro.getId(), request.servicosIds(), request.data(), request.horaInicio()));
        } else {
            if (request.nomeCliente() == null || request.nomeCliente().isBlank()
                    || request.telefoneCliente() == null || request.telefoneCliente().isBlank()) {
                throw erro(HttpStatus.BAD_REQUEST, "Informe nome e telefone do cliente avulso");
            }
            var servicos = validacao.validarServicos(request.servicosIds());
            if (!request.data().atTime(request.horaInicio()).isAfter(LocalDateTime.now())) {
                throw erro(HttpStatus.BAD_REQUEST, "O atendimento deve ser no futuro");
            }
            if (!disponibilidade.horariosDisponiveis(barbeiro.getUsuario(), barbeiro.getId(), request.data(), servicos)
                    .contains(request.horaInicio())) {
                throw erro(HttpStatus.CONFLICT, "Horario indisponivel para estes servicos");
            }
            agendamento = new Agendamento();
            agendamento.setBarbeiro(barbeiro);
            agendamento.setNomeCliente(request.nomeCliente().trim());
            agendamento.setTelefoneCliente(request.telefoneCliente().trim());
            agendamento.setServico(servicos);
            agendamento.setData(request.data());
            agendamento.setHoraInicio(request.horaInicio());
            int duracao = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum();
            agendamento.setDuracaoServicoMinutos(duracao);
            agendamento.setHoraFinalizacao(request.horaInicio().plusMinutes(duracao));
            agendamento.setPrecoTotal(servicos.stream().map(Servico::getPreco).reduce(BigDecimal.ZERO, BigDecimal::add));
            agendamento.setStatus(StatusAgendamento.AGENDADO);
            agendamento.setPagamentoRecebido(false);
            agendamento.setCriadoEm(LocalDateTime.now());
            agendamentos.save(agendamento);
        }
        agendamento.setCriadoPorUsuario(barbeiro.getUsuario());
        return new CriacaoResponse(agendamento.getId(), "AGENDAMENTO", agendamento.getData(),
                agendamento.getHoraInicio(), agendamento.getHoraFinalizacao());
    }

    @Transactional
    public CriacaoResponse criarBloqueio(String login, CriarBloqueioAgendaRequest request) {
        var barbeiro = bloquearBarbeiro(login);
        var dia = grade.consultar(login, request.data(), null);
        if (!dia.podeCriar() || !request.data().atTime(request.horaInicio()).isAfter(LocalDateTime.now())
                || !request.horaFim().isAfter(request.horaInicio())
                || request.horaInicio().isBefore(dia.inicio()) || request.horaFim().isAfter(dia.fim())) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um periodo futuro dentro da jornada do barbeiro");
        }
        if (!dia.horarios().contains(request.horaInicio())) {
            throw erro(HttpStatus.BAD_REQUEST, "Inicio do bloqueio deve seguir a grade configurada");
        }
        if (dia.indisponibilidades().stream().anyMatch(p -> p.inicio() == null || p.fim() == null
                || conflito(request.horaInicio(), request.horaFim(), p.inicio(), p.fim()))) {
            throw erro(HttpStatus.CONFLICT, "Periodo ja bloqueado ou fora do funcionamento");
        }
        boolean ocupado = agendamentos.findAllByBarbeiro_idAndData(barbeiro.getId(), request.data()).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO && a.getStatus() != StatusAgendamento.NAO_COMPARECEU)
                .anyMatch(a -> conflito(request.horaInicio(), request.horaFim(), a.getHoraInicio(), a.getHoraFinalizacao()));
        if (ocupado) throw erro(HttpStatus.CONFLICT, "Existe atendimento neste periodo");
        var bloqueio = new BloqueioBarbeiro();
        bloqueio.setBarbeiro(barbeiro); bloqueio.setDataBloqueio(request.data());
        bloqueio.setHoraInicio(request.horaInicio()); bloqueio.setHoraFim(request.horaFim());
        bloqueios.save(bloqueio);
        return new CriacaoResponse(bloqueio.getId(), "BLOQUEIO", request.data(), request.horaInicio(), request.horaFim());
    }

    @Transactional
    public void removerBloqueio(String login, Long id) {
        var barbeiro = bloquearBarbeiro(login);
        var bloqueio = bloqueios.findByIdAndBarbeiro_Id(id, barbeiro.getId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Bloqueio nao encontrado"));
        bloqueios.delete(bloqueio);
    }

    private Barbeiro bloquearBarbeiro(String login) {
        var barbeiro = barbeiros.findByUsuario_Login(login)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var bloqueado = barbeiros.findByIdParaAgendar(barbeiro.getId()).orElseThrow();
        if (!bloqueado.isAtivo()) throw erro(HttpStatus.FORBIDDEN, "Barbeiro inativo");
        return bloqueado;
    }

    private boolean conflito(LocalTime inicio, LocalTime fim, LocalTime outroInicio, LocalTime outroFim) {
        return outroInicio == null || outroFim == null || inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
