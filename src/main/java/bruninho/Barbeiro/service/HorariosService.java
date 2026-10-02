package bruninho.Barbeiro.service;


import bruninho.Barbeiro.Controller.Admin.requests.CriarJornadasDosBarbeiros;
import bruninho.Barbeiro.Controller.Admin.requests.HorarioFuncionamentoBarber;
import bruninho.Barbeiro.Controller.Admin.requests.ListJornadaSemanaBarbeiros;
import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepository;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HorariosService {

    private JornadaBarbeiroRepository jornadaBarbeiroRepository;
    private HorarioFuncionamentoRepository horarioFuncionamentoRepository;
    private BarbeiroRepository barbeiroRepository;

    public HorariosService(
            JornadaBarbeiroRepository jornadaBarbeiroRepository,
            HorarioFuncionamentoRepository horarioFuncionamentoRepository, BarbeiroRepository barbeiroRepository
    ) {
        this.jornadaBarbeiroRepository = jornadaBarbeiroRepository;
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;
        this.barbeiroRepository = barbeiroRepository;
    }

    @Transactional
    public void horarioDeFuncionamentoBarbeiaria(List<HorarioFuncionamentoBarber> horarioFuncionamento) {
        for (HorarioFuncionamentoBarber horario : horarioFuncionamento) {
            HorarioFuncionamento horarioFuncionamentoEntity = new HorarioFuncionamento();
            horarioFuncionamentoEntity.setDiaSemana(horario.diaSemana());
            horarioFuncionamentoEntity.setFechado(horario.fechado());
            horarioFuncionamentoEntity.setHoraAbertura(horario.horarioInicio());
            horarioFuncionamentoEntity.setHoraFechamento(horario.horarioFim());
            if(horarioFuncionamentoEntity.isFechado()){
                horarioFuncionamentoEntity.setHoraAbertura(null);
                horarioFuncionamentoEntity.setHoraFechamento(null);
            }else {
                if (horarioFuncionamentoEntity.getHoraAbertura() == null || horarioFuncionamentoEntity.getHoraFechamento() == null){
                    throw new RuntimeException("deve ser os horarios corretamente ou marca como fechado");
                }
                if (!horarioFuncionamentoEntity.getHoraAbertura().isBefore(horarioFuncionamentoEntity.getHoraFechamento())){
                    throw new RuntimeException("o horario de fechamento deve ser após o horario de abertura");
                }
            }
            horarioFuncionamentoRepository.save(horarioFuncionamentoEntity);
        }
    }


    @Transactional
    public void criarJornadaBarbeiro(ListJornadaSemanaBarbeiros listJornadaSemanaBarbeiros) {
        Barbeiro barbeiroEntity = barbeiroRepository.findById(listJornadaSemanaBarbeiros.barbeiroid()).orElseThrow(() -> new RuntimeException("não localizado o barbeiro"));

        for(CriarJornadasDosBarbeiros jornada : listJornadaSemanaBarbeiros.jornadaBarbeiros()){
            JornadaBarbeiro jornadaBarbeiro = new JornadaBarbeiro();
            jornadaBarbeiro.setBarbeiro(barbeiroEntity);
            jornadaBarbeiro.setDiaSemana(jornada.getDiaSemana());
            jornadaBarbeiro.setHoraInicio(jornada.getHoraInicio());
            jornadaBarbeiro.setHoraFim(jornada.getHoraFim());
            jornadaBarbeiro.setIntervaloInicio(jornada.getInicioIntervalo());
            jornadaBarbeiro.setIntervaloFim(jornada.getFimIntervalo());
            jornadaBarbeiro.setAtivo(true);
            HorarioFuncionamento horarioFuncionamentoEntity = horarioFuncionamentoRepository.findByDiaSemana(jornadaBarbeiro.getDiaSemana());
            if (horarioFuncionamentoEntity.isFechado()) {

                jornadaBarbeiro.setFolga(true);
                jornadaBarbeiro.setIntervaloInicio(null);
                jornadaBarbeiro.setIntervaloFim(null);
                jornadaBarbeiro.setHoraInicio(null);
                jornadaBarbeiro.setHoraFim(null);

            } else {
                jornadaBarbeiro.setFolga(false);
                if (jornadaBarbeiro.getIntervaloInicio() != null
                        && jornadaBarbeiro.getIntervaloFim() != null) {

                    if (!jornadaBarbeiro.getIntervaloInicio()
                            .isBefore(jornadaBarbeiro.getIntervaloFim())) {
                        throw new RuntimeException("Intervalo inválido");
                    }

                    if (jornadaBarbeiro.getIntervaloInicio()
                            .isBefore(jornadaBarbeiro.getHoraInicio())) {
                        throw new RuntimeException("Intervalo começa antes da jornada");
                    }

                    if (jornadaBarbeiro.getIntervaloFim()
                            .isAfter(jornadaBarbeiro.getHoraFim())) {
                        throw new RuntimeException("Intervalo termina após a jornada");
                    }
                }else{
                    jornadaBarbeiro.setIntervaloInicio(null);
                    jornadaBarbeiro.setIntervaloFim(null);
                }
                if (jornadaBarbeiro.getHoraInicio() == null
                        || jornadaBarbeiro.getHoraFim() == null) {
                    throw new RuntimeException(
                            "Horários da jornada são obrigatórios, ou marcar como folga"
                    );
                }

                if (jornadaBarbeiro.getHoraInicio()
                        .isBefore(horarioFuncionamentoEntity.getHoraAbertura())) {

                    throw new RuntimeException(
                            "O barbeiro não pode iniciar antes da abertura da barbearia"
                    );
                }

                if (jornadaBarbeiro.getHoraFim()
                        .isAfter(horarioFuncionamentoEntity.getHoraFechamento())) {

                    throw new RuntimeException(
                            "O barbeiro não pode finalizar o expediente após o fechamento da barbearia"
                    );
                }

                if (jornadaBarbeiro.getHoraFim()
                        .isBefore(jornadaBarbeiro.getHoraInicio())) {

                    throw new RuntimeException(
                            "O horário final deve ser após o horário inicial"
                    );
                }
            }

            jornadaBarbeiroRepository.save(jornadaBarbeiro);


        }
    }




}
