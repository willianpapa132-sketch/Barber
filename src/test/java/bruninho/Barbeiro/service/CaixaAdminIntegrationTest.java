package bruninho.Barbeiro.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:caixa_admin_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaixaAdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired bruninho.Barbeiro.repository.CaixaRepository caixas;
    @Autowired bruninho.Barbeiro.repository.MovimentoCaixaRepository movimentos;

    // Sem transacao no teste: cada chamada HTTP deve abrir sua propria transacao.
    @Test
    void semDadosPermiteAbrirCaixaComSaldoZeroEConsultar() throws Exception {
        mvc.perform(get("/api/admin/caixa/historico").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/admin/caixa").with(user("admin").roles("ADMIN")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Nao existe caixa aberto"));

        mvc.perform(post("/api/admin/caixa").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"saldoInicial\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ABERTO"))
                .andExpect(jsonPath("$.saldoAtual").value(0));

        mvc.perform(get("/api/admin/caixa").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoInicial").value(0))
                .andExpect(jsonPath("$.totalEntradas").value(0))
                .andExpect(jsonPath("$.totalSangrias").value(0));

        mvc.perform(post("/api/admin/caixa").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"saldoInicial\":100}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/admin/caixa/fechamento").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FECHADO"));

        mvc.perform(post("/api/admin/caixa").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"saldoInicial\":100}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/caixa/historico").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").isNumber())
                .andExpect(jsonPath("$[0].status").value("ABERTO"))
                .andExpect(jsonPath("$[0].saldoInicial").value(100))
                .andExpect(jsonPath("$[0].abertoEm").isNotEmpty())
                .andExpect(jsonPath("$[0].fechadoEm").isEmpty())
                .andExpect(jsonPath("$[1].status").value("FECHADO"))
                .andExpect(jsonPath("$[1].fechadoEm").isNotEmpty())
                .andExpect(jsonPath("$[1].totalEntradas").value(0))
                .andExpect(jsonPath("$[1].saldoFinal").value(0));

        var historico = caixas.findAll(org.springframework.data.domain.Sort.by("id"));
        var fechado = historico.getFirst();
        var aberto = historico.getLast();
        mvc.perform(get("/api/admin/caixa/" + fechado.getId() + "/movimentacoes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        movimentos.save(new bruninho.Barbeiro.domain.MovimentoCaixa(null, fechado, null,
                bruninho.Barbeiro.domain.TipoMovimentoCaixa.ENTRADA, new java.math.BigDecimal("50.00"),
                java.time.LocalDateTime.of(2026, 10, 20, 10, 0), "Atendimento finalizado"));
        movimentos.save(new bruninho.Barbeiro.domain.MovimentoCaixa(null, fechado, null,
                bruninho.Barbeiro.domain.TipoMovimentoCaixa.SANGRIA, new java.math.BigDecimal("20.00"),
                java.time.LocalDateTime.of(2026, 10, 20, 11, 0), "Compra de materiais"));
        mvc.perform(post("/api/admin/caixa/sangrias").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"valor\":25,\"observacao\":\"Outro caixa\"}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/admin/caixa/" + fechado.getId() + "/movimentacoes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].tipo").value("SANGRIA"))
                .andExpect(jsonPath("$[0].valor").value(20))
                .andExpect(jsonPath("$[0].observacao").value("Compra de materiais"))
                .andExpect(jsonPath("$[1].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$[1].valor").value(50));
        mvc.perform(get("/api/admin/caixa/" + aberto.getId() + "/movimentacoes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].observacao").value("Outro caixa"));
        mvc.perform(get("/api/admin/caixa/999999/movimentacoes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/caixa/" + fechado.getId() + "/movimentacoes").with(user("barbeiro").roles("BARBEIRO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin/caixa/" + fechado.getId() + "/movimentacoes").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }
}
