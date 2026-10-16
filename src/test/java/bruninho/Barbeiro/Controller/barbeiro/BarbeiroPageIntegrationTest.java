package bruninho.Barbeiro.Controller.barbeiro;

import bruninho.Barbeiro.Controller.barbeiro.agenda.*;
import bruninho.Barbeiro.Controller.barbeiro.home.*;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.service.ConsultaAgendaBarbeiroService;
import bruninho.Barbeiro.service.HomeBarbeiroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BarbeiroPageIntegrationTest {
    @Autowired MockMvc mvc;
    @MockitoBean HomeBarbeiroService home;
    @MockitoBean ConsultaAgendaBarbeiroService agenda;
    private final LocalDate dia = LocalDate.of(2026, 10, 15);

    @Test
    void homeVaziaRenderizaNomeIndicadoresEMensagem() throws Exception {
        when(home.homeDoBarbeiro("barbeiro@demo.com")).thenReturn(new BarbeiroHomeResponse(
                new BarbeiroHomeResponse.BarbeiroResumo(1L, "Willian Silva", null), dia, 0, 0, 0, List.of()));
        mvc.perform(get("/barbeiro/home").with(user("barbeiro@demo.com").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(view().name("barbeiro/home"))
                .andExpect(content().string(containsString("Willian Silva")))
                .andExpect(content().string(containsString("15 de outubro de 2026")))
                .andExpect(content().string(containsString("Nenhum próximo agendamento para hoje")))
                .andExpect(content().string(containsString("/barbeiro/agenda")));
    }

    @Test
    void homeComAtendimentoRenderizaDadosESemEstadoVazio() throws Exception {
        var item = new ProximoAgendamentoBarbeiroResponse(9L, dia, LocalTime.of(14, 30),
                LocalTime.of(14, 55), 25, "Carlos Mendes", null, List.of("Corte", "Barba"), StatusAgendamento.AGENDADO);
        when(home.homeDoBarbeiro("barbeiro@demo.com")).thenReturn(new BarbeiroHomeResponse(
                new BarbeiroHomeResponse.BarbeiroResumo(1L, "Willian", null), dia, 6, 2, 1, List.of(item)));
        mvc.perform(get("/barbeiro/home").with(user("barbeiro@demo.com").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Carlos Mendes")))
                .andExpect(content().string(containsString("14:30")))
                .andExpect(content().string(containsString("/barbeiro/agendamentos/9/finalizacao")))
                .andExpect(content().string(containsString("Corte + Barba")))
                .andExpect(content().string(not(containsString("Nenhum próximo agendamento para hoje"))));
    }

    @Test
    void agendaRenderizaFiltrosAtendimentoEPaginacao() throws Exception {
        var item = new AgendamentoAgendaBarbeiroResponse(9L, LocalTime.of(14, 30), LocalTime.of(14, 45),
                15, "Carlos Mendes", List.of("Corte"), StatusAgendamento.AGENDADO);
        when(agenda.consultar("barbeiro@demo.com", dia, StatusAgendamento.AGENDADO, 0, 20))
                .thenReturn(new AgendaBarbeiroResponse(dia, StatusAgendamento.AGENDADO, 9L,
                        List.of(item), 0, 20, 21, 2, true));
        mvc.perform(get("/barbeiro/agenda").param("data", dia.toString()).param("status", "AGENDADO")
                        .with(user("barbeiro@demo.com").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(view().name("barbeiro/agenda"))
                .andExpect(content().string(containsString("Carlos Mendes")))
                .andExpect(content().string(containsString("/barbeiro/agendamentos/9/finalizacao")))
                .andExpect(content().string(containsString("Próxima")))
                .andExpect(content().string(containsString("pagina=1")));
    }

    @Test
    void agendaVaziaRenderizaSemFiltroInformado() throws Exception {
        when(agenda.consultar("barbeiro@demo.com", null, null, 0, 20))
                .thenReturn(new AgendaBarbeiroResponse(dia, null, null, List.of(), 0, 20, 0, 0, false));
        mvc.perform(get("/barbeiro/agenda").with(user("barbeiro@demo.com").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum agendamento encontrado")));
    }

    @Test
    void paginaDeFinalizacaoCarregaTelaEEndpointDoAgendamento() throws Exception {
        mvc.perform(get("/barbeiro/agendamentos/9/finalizacao").with(user("barbeiro@demo.com").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(view().name("barbeiro/finalizacao"))
                .andExpect(content().string(containsString("data-api-url=\"/api/barbeiro/agendamentos/9/finalizacao\"")))
                .andExpect(content().string(containsString("/js/barbeiro-finalizacao.js")))
                .andExpect(content().string(containsString("Finalizar atendimento")));
    }

    @Test
    void clienteNaoPodeAbrirPaginasDoBarbeiro() throws Exception {
        mvc.perform(get("/barbeiro/home").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/barbeiro/agenda").with(user("cliente").roles("CLIENTE"))).andExpect(status().isForbidden());
        mvc.perform(get("/barbeiro/agendamentos/9/finalizacao").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(home, agenda);
    }
}
