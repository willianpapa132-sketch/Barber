package bruninho.Barbeiro.service;

import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.exception.BusinessException;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.Usuario;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class DisponibilidadeAgendamentosService {
    private final HorarioFuncionamentoRepository funcionamento;
    private final JornadaBarbeiroRepository jornadas;
    private final AgendamentoRepository agendamentos;
    private final BloqueioBarbeiroRepository bloqueios;
    private final ConfiguracaoBarbeariaRepository configuracoes;
    private final PlanoMensalClienteRepository planos;
    private final ValidacaoAgendamentoService validacao;

    public DisponibilidadeAgendamentosService(HorarioFuncionamentoRepository funcionamento,
            JornadaBarbeiroRepository jornadas, AgendamentoRepository agendamentos,
            BloqueioBarbeiroRepository bloqueios, ConfiguracaoBarbeariaRepository configuracoes,
            PlanoMensalClienteRepository planos, ValidacaoAgendamentoService validacao) {
        this.funcionamento = funcionamento;
        this.jornadas = jornadas;
        this.agendamentos = agendamentos;
        this.bloqueios = bloqueios;
        this.configuracoes = configuracoes;
        this.planos = planos;
        this.validacao = validacao;
    }

    public List<LocalTime> horariosDiaDisponivel(Long usuarioId, Long clienteId, Long barbeiroId,
                                                LocalDate dia, List<Long> servicosIds) {
        var cliente = validacao.validarCliente(usuarioId, clienteId);
        validacao.validarBarbeiro(barbeiroId);
        var servicos = validacao.validarServicos(servicosIds);
        if (dia == null) throw new BusinessException("Informe a data");
        return horariosDisponiveis(cliente.getUsuario(), barbeiroId, dia, servicos);
    }

    public List<LocalDate> diasDisponiveis(Long usuarioId, Long clienteId, Long barbeiroId, List<Long> servicosIds) {
        var cliente = validacao.validarCliente(usuarioId, clienteId);
        validacao.validarBarbeiro(barbeiroId);
        var servicos = validacao.validarServicos(servicosIds);
        List<LocalDate> dias = new ArrayList<>();
        LocalDate hoje = LocalDate.now();
        int antecedencia = antecedencia(cliente.getUsuario());
        // Preserva a regra existente: zero dias desabilita a agenda.
        if (antecedencia == 0) return dias;
        LocalDate limite = hoje.plusDays(antecedencia - 1L);
        for (LocalDate dia = hoje; !dia.isAfter(limite); dia = dia.plusDays(1)) {
            if (!calcularHorarios(barbeiroId, dia, servicos).isEmpty()) dias.add(dia);
        }
        return dias;
    }

    public List<LocalTime> horariosDisponiveis(Usuario usuario, Long barbeiroId, LocalDate dia, Set<Servico> servicos) {
        LocalDate hoje = LocalDate.now();
        int antecedencia = antecedencia(usuario);
        if (antecedencia == 0 || dia.isBefore(hoje) || dia.isAfter(hoje.plusDays(antecedencia - 1L))) return List.of();
        return calcularHorarios(barbeiroId, dia, servicos);
    }

    private int antecedencia(Usuario usuario) {
        var config = configuracoes.findById(1L)
                .orElseThrow(() -> new NotFoundException("Configuração da barbearia não encontrada"));
        Integer dias = config.getDiasMaximoAntecedentia();
        if (Boolean.TRUE.equals(planos.existsByUsuarioAndAtivoTrue(usuario)))
            dias = planos.findByUsuarioAndAtivoTrue(usuario).getDiasMaximoAntecedencia();
        if (dias == null || dias < 0) throw new BusinessException("Antecedência não configurada corretamente");
        return Math.min(dias, 30);
    }

    private List<LocalTime> calcularHorarios(Long barbeiroId, LocalDate dia, Set<Servico> servicos) {
        var horario = funcionamento.findByDiaSemana(dia.getDayOfWeek());
        var jornada = jornadas.findByDiaSemanaAndBarbeiro_id(dia.getDayOfWeek(), barbeiroId).orElse(null);
        if (horario == null || jornada == null || !Boolean.FALSE.equals(horario.getFechado())
                || Boolean.TRUE.equals(jornada.getFolga()) || Boolean.FALSE.equals(jornada.getAtivo())
                || bloqueios.existsByDataBloqueioAndBarbeiro_Id(dia, barbeiroId)) return List.of();
        if (horario.getIntervaloMinimoEntreAgendamentos() == null || horario.getIntervaloMinimoEntreAgendamentos() <= 0
                || horario.getHoraAbertura() == null || horario.getHoraFechamento() == null
                || jornada.getHoraInicio() == null || jornada.getHoraFim() == null)
            throw new BusinessException("Horários de funcionamento ou jornada inválidos");
        int inicio = Math.max(horario.getHoraAbertura().toSecondOfDay(), jornada.getHoraInicio().toSecondOfDay());
        int fim = Math.min(horario.getHoraFechamento().toSecondOfDay(), jornada.getHoraFim().toSecondOfDay());
        int duracao = servicos.stream().mapToInt(Servico::getDuracaoMinutos).sum() * 60;
        if (duracao <= 0 || duracao >= 86400) throw new BusinessException("Duração inválida");
        long passo = horario.getIntervaloMinimoEntreAgendamentos().longValue() * 60;
        var ocupados = agendamentos.findAllByBarbeiro_idAndData(barbeiroId, dia);
        List<LocalTime> resultado = new ArrayList<>();
        LocalDateTime agora = LocalDateTime.now();
        for (long segundo = inicio; segundo + duracao <= fim; segundo += passo) {
            LocalTime hora = LocalTime.ofSecondOfDay(segundo);
            LocalTime termino = LocalTime.ofSecondOfDay(segundo + duracao);
            if (!dia.atTime(hora).isAfter(agora)) continue;
            if (jornada.getIntervaloInicio() != null && jornada.getIntervaloFim() != null
                    && conflito(hora, termino, jornada.getIntervaloInicio(), jornada.getIntervaloFim())) continue;
            boolean conflito = ocupados.stream().filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                    .anyMatch(a -> conflito(hora, termino, a.getHoraInicio(), a.getHoraFinalizacao()));
            if (!conflito) resultado.add(hora);
        }
        return resultado;
    }

    private boolean conflito(LocalTime inicio, LocalTime fim, LocalTime outroInicio, LocalTime outroFim) {
        return inicio.isBefore(outroFim) && fim.isAfter(outroInicio);
    }
}
