package bruninho.Barbeiro.service;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
@Service
public class AgendamentoStatusService {
    private final AgendamentoRepository agendamentos;
    private final ComandaRepository comandas;
    private final java.time.Clock clock;
    public AgendamentoStatusService(AgendamentoRepository a,ComandaRepository c,bruninho.Barbeiro.config.AppProperties props){agendamentos=a;comandas=c;clock=java.time.Clock.system(java.time.ZoneId.of(props.getZone()));}
    @Transactional public void cancelar(String login,Long id){alterar(login,id,StatusAgendamento.CANCELADO);}
    @Transactional public void registrarNaoComparecimento(String login,Long id){alterar(login,id,StatusAgendamento.NAO_COMPARECEU);}
    private void alterar(String login,Long id,StatusAgendamento status){
        var a=agendamentos.findParaFinalizar(id,login).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Agendamento nao encontrado"));
        if(a.getStatus()!=StatusAgendamento.AGENDADO||comandas.existsByAgendamento_Id(id)||Boolean.TRUE.equals(a.getPagamentoRecebido()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Agendamento ja encerrado ou com comanda fechada");
        if(status==StatusAgendamento.NAO_COMPARECEU&&(a.getData()==null||a.getHoraInicio()==null||LocalDateTime.now(clock).isBefore(a.getData().atTime(a.getHoraInicio()))))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Aguarde o inicio do atendimento");
        a.setStatus(status);a.setAtualizadoEm(LocalDateTime.now(clock));
    }
}
