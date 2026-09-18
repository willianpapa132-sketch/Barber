package bruninho.Barber.web;

import bruninho.Barber.domain.BarberShopConfig;
import bruninho.Barber.repository.BarberShopConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired BarberShopConfigRepository configs;

    @BeforeEach
    void setUp() {
        if (configs.findById(1L).isEmpty()) {
            BarberShopConfig config = new BarberShopConfig();
            config.setNome("Barbearia Teste");
            config.setMinAntecedenciaMinutos(30);
            config.setHorizonteDias(60);
            configs.save(config);
        }
    }

    @Test
    void publicoAcessaAgendamentoEAdminExigeLogin() throws Exception {
        mvc.perform(get("/agendar")).andExpect(status().isOk());
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection());
    }

    @Test
    void barbeiroNaoAcessaAdminFinancasNemEscrita() throws Exception {
        mvc.perform(get("/admin/caixa").with(user("b").roles("BARBEIRO"))).andExpect(status().isForbidden());
        mvc.perform(post("/admin/agenda/1/status").with(user("b").roles("BARBEIRO")).with(csrf()).param("status", "CANCELADO")).andExpect(status().isForbidden());
    }

    @Test
    void adminAcessaPainel() throws Exception {
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
}
