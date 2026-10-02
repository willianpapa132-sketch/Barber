package bruninho.Barbeiro.service;



import bruninho.Barbeiro.repository.AgendamentoRepository;
import org.springframework.stereotype.Service;




@Service
public class AgendamentoService {

    private AgendamentoRepository agendamentoRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository) {
        this.agendamentoRepository = agendamentoRepository;
    }



    public void agendar(){

    }

}
