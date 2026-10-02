package bruninho.Barbeiro.service;


import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DisponibilidadeAgendamentosService {
    private HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private JornadaBarbeiroRepository  jornadaBarbeiroRepository;
    private AgendamentoRepository agendamentoRepository;
    private BloqueioBarbeiroRepository  bloqueioBarbeiroRepository;
    private ConfiguracaoBarbeariaRepository configuracaoBarbeariaRepository;


    private ServicoRepository servicoRepository;


    public DisponibilidadeAgendamentosService(
            HorarioFuncionamentoRepository horarioFuncionamentoRepository,
            JornadaBarbeiroRepository jornadaBarbeiroRepository,
            AgendamentoRepository agendamentoRepository,
            BloqueioBarbeiroRepository bloqueioBarbeiroRepository,
            ServicoRepository servicoRepository,
            ConfiguracaoBarbeariaRepository configuracaoBarbeariaRepository
    ) {
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.jornadaBarbeiroRepository = jornadaBarbeiroRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.bloqueioBarbeiroRepository = bloqueioBarbeiroRepository;
        this.servicoRepository = servicoRepository;
        this.configuracaoBarbeariaRepository = configuracaoBarbeariaRepository;
    }





    public List<LocalTime> horariosDiaDisponivel(Long clienteid, Long barbeiroid, LocalDate dia, List<Long>servicosid ) {
        List<LocalTime> horariosDisponivel = new ArrayList<>();
        Integer totalMinAgendamento = 0;
        DayOfWeek diaSemana = dia.getDayOfWeek();
        HorarioFuncionamento horarioFuncionamento = horarioFuncionamentoRepository.findByDiaSemana(diaSemana);
        JornadaBarbeiro jornadaBarbeiro = jornadaBarbeiroRepository.findByDiaSemanaAndBarbeiro_id(diaSemana, barbeiroid)
                .orElseThrow(() -> new RuntimeException("não localizado esse dia da semana para o barbeiro"));


        for (Long servicoid : servicosid) {
            Servico servico = servicoRepository.findById(servicoid).orElseThrow(
                    () -> new RuntimeException("Não Localizado o Servico"));
            totalMinAgendamento += servico.getDuracaoMinutos();
        }
        if (horarioFuncionamento.getFechado()) {
            throw new RuntimeException("barbearia fechada");
        }
        if (jornadaBarbeiro.getFolga()) {
            throw new RuntimeException("barbeiro esta de folga no dia selecionado");
        }
        if (bloqueioBarbeiroRepository.findByDataBloqueioAndBarbeiro_id(dia, barbeiroid)) {
            throw new RuntimeException("barbeiro esta bloqueado nessa data escolha outro barbeiro");
        }

        LocalTime inicio = jornadaBarbeiro.getHoraInicio();
        LocalTime fim = jornadaBarbeiro.getHoraFim();


        List<Agendamento> agendamentosdoDia = agendamentoRepository.findAllByBarbeiro_idAndData(barbeiroid, dia);
        while (inicio.isBefore(fim)) {

            LocalTime horarioFimAgendamento =
                    inicio.plusMinutes(totalMinAgendamento);

            if (horarioFimAgendamento.isAfter(fim)) {
                break;
            }

            LocalTime inicioIntervalo = jornadaBarbeiro.getIntervaloInicio();
            LocalTime fimIntervalo = jornadaBarbeiro.getIntervaloFim();

            boolean disponivel = true;

            if (jornadaBarbeiro.getIntervaloInicio() != null && jornadaBarbeiro.getIntervaloFim() != null) {
                boolean pegaHoraDoAlmoco =
                        inicio.isBefore(fimIntervalo)
                                && horarioFimAgendamento.isAfter(inicioIntervalo);
                if (pegaHoraDoAlmoco) {
                    disponivel = false;
                }

            }
            if (disponivel) {
                for (Agendamento agendamento : agendamentosdoDia) {
                    LocalTime horarioInicio = agendamento.getHoraInicio();
                    LocalTime horarioFim = agendamento.getHoraFinalizacao();
                    boolean conflito =
                            inicio.isBefore(horarioFim)
                                    && horarioFimAgendamento.isAfter(horarioInicio);

                    if (conflito) {
                        disponivel = false;
                        break;
                    }
                }
            }
            if (disponivel) {
                horariosDisponivel.add(inicio);
            }
            inicio = inicio.plusMinutes(horarioFuncionamento.getIntervaloMinimoEntreAgendamentos());

        }
        return horariosDisponivel;
    }

    public List<LocalDate> diasDisponiveis(Long barbeiro_id){
        List<LocalDate> dias = new ArrayList<>();
        LocalDate inicio = LocalDate.now();
        ConfiguracaoBarbearia configuracaoBarbearia = configuracaoBarbeariaRepository.findById(1L).orElse(null);
        if(configuracaoBarbearia.getDiasMaximoAntecedentia() != 0){
            LocalDate fim = inicio.plusDays(configuracaoBarbearia.getDiasMaximoAntecedentia());
            while (!inicio.isAfter(fim)) {
                DayOfWeek diaSemana = inicio.getDayOfWeek();

                HorarioFuncionamento funcionamento =
                        horarioFuncionamentoRepository.findByDiaSemana(diaSemana);

                var jornadaOpt =
                        jornadaBarbeiroRepository.findByDiaSemanaAndBarbeiro_id(diaSemana, barbeiro_id);
                Boolean bloqueioBarbeiroTrue = bloqueioBarbeiroRepository.findByDataBloqueioAndBarbeiro_id(inicio,barbeiro_id);

                if(jornadaOpt.isPresent() && !bloqueioBarbeiroTrue) {
                    JornadaBarbeiro jornadaBarbeiro = jornadaOpt.get();
                    if (!funcionamento.getFechado() && !jornadaBarbeiro.getFolga()) {
                        dias.add(inicio);
                    }
                }

                inicio = inicio.plusDays(1);

            }
        }
        return dias;
    }

}
