package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.barbeiro.agenda.AgendamentoAgendaBarbeiroResponse;
import bruninho.Barbeiro.domain.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
class SemanaPagamentoPlanoTest {
 private Agendamento agenda(LocalDate data){var a=new Agendamento();a.setData(data);a.setStatus(StatusAgendamento.AGENDADO);var p=new PlanoMensalCliente();p.setAtivo(true);p.setProximoVencimento(LocalDate.of(2026,12,2));a.setPlanoMensalCliente(p);return a;}
 @Test void incluiSegundaEDomingoMesmoEntreMeses(){assertTrue(AgendamentoAgendaBarbeiroResponse.de(agenda(LocalDate.of(2026,11,30))).semanaPagamentoPlano());assertTrue(AgendamentoAgendaBarbeiroResponse.de(agenda(LocalDate.of(2026,12,6))).semanaPagamentoPlano());}
 @Test void excluiDiasForaDaSemana(){assertFalse(AgendamentoAgendaBarbeiroResponse.de(agenda(LocalDate.of(2026,11,29))).semanaPagamentoPlano());assertFalse(AgendamentoAgendaBarbeiroResponse.de(agenda(LocalDate.of(2026,12,7))).semanaPagamentoPlano());}
 @Test void excluiSemPlanoInativoEConcluido(){var a=agenda(LocalDate.of(2026,12,2));a.getPlanoMensalCliente().setAtivo(false);assertFalse(AgendamentoAgendaBarbeiroResponse.de(a).semanaPagamentoPlano());a.getPlanoMensalCliente().setAtivo(true);a.setStatus(StatusAgendamento.CONCLUIDO);assertFalse(AgendamentoAgendaBarbeiroResponse.de(a).semanaPagamentoPlano());a.setPlanoMensalCliente(null);assertFalse(AgendamentoAgendaBarbeiroResponse.de(a).semanaPagamentoPlano());}
 @Test void pagamentoQueAvancaVencimentoRemoveAviso(){var a=agenda(LocalDate.of(2026,12,2));a.getPlanoMensalCliente().setProximoVencimento(LocalDate.of(2027,1,2));assertFalse(AgendamentoAgendaBarbeiroResponse.de(a).semanaPagamentoPlano());}
}
