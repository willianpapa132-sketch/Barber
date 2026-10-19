package bruninho.Barbeiro.service;



import bruninho.Barbeiro.Controller.Admin.requests.HorarioFuncionamentoBarber;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class HorariosService {


    private HorarioFuncionamentoRepository horarioFuncionamentoRepository;


    public HorariosService(
            HorarioFuncionamentoRepository horarioFuncionamentoRepository, BarbeiroRepository barbeiroRepository
    ) {
        this.horarioFuncionamentoRepository = horarioFuncionamentoRepository;

    }

    @Transactional
    public void horarioDeFuncionamentoBarbeiaria(List<HorarioFuncionamentoBarber> horarioFuncionamento) {
        for (HorarioFuncionamentoBarber horario : horarioFuncionamento) {
            HorarioFuncionamento horarioFuncionamentoEntity = new HorarioFuncionamento();
            horarioFuncionamentoEntity.setDiaSemana(horario.diaSemana());
            horarioFuncionamentoEntity.setFechado(horario.fechado());
            horarioFuncionamentoEntity.setHoraAbertura(horario.horarioInicio());
            horarioFuncionamentoEntity.setHoraFechamento(horario.horarioFim());
            if(horarioFuncionamentoEntity.getFechado()){
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






}
