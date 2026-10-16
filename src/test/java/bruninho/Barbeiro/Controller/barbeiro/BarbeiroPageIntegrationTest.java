package bruninho.Barbeiro.Controller.barbeiro;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class BarbeiroPageIntegrationTest {
    @Autowired MockMvc mvc;
    private final String[] rotas = { "/barbeiro", "/barbeiro/home", "/barbeiro/agenda",
            "/barbeiro/agenda?data=2026-10-15&status=AGENDADO", "/barbeiro/agendamentos/9/finalizacao" };

    @Test
    void paginasDoBarbeiroAbremAplicacaoAngularInclusiveAcessoDireto() throws Exception {
        for (String rota : rotas) {
            mvc.perform(get(rota).with(user("barbeiro@demo.com").roles("BARBEIRO")))
                    .andExpect(status().isOk())
                    .andExpect(forwardedUrl("/client/index.html"));
        }
    }

    @Test
    void clienteNaoPodeAbrirPaginasDoBarbeiro() throws Exception {
        for (String rota : rotas) {
            mvc.perform(get(rota).with(user("cliente").roles("CLIENTE")))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void visitantePrecisaEntrarAntesDeAbrirPaginasDoBarbeiro() throws Exception {
        for (String rota : rotas) {
            mvc.perform(get(rota)).andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/login"));
        }
    }
}
