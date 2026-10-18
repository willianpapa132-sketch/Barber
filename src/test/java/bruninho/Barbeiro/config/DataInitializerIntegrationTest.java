package bruninho.Barbeiro.config;

import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "app.demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:initializer_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class DataInitializerIntegrationTest {
    @Autowired PlanoMensalRepository planos;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ServicoRepository servicos;
    @Autowired UsuarioRepository usuarios;
    @Autowired MockMvc mvc;
    @Autowired @Qualifier("bootstrap") CommandLineRunner bootstrap;

    @Test
    void iniciaComPlanosDemoEComissaoDoBarbeiro() {
        assertThat(planos.findAll()).hasSize(3);
        assertThat(barbeiros.findByUsuario_Login("barbeiro@demo.com").orElseThrow().getPorcentagemComissao()).isEqualTo(40);
        assertThat(servicos.findByNomeIgnoreCase("Corte").orElseThrow().getDuracaoMinutos()).isEqualTo(15);
        assertThat(servicos.findByNomeIgnoreCase("Barba").orElseThrow().getDuracaoMinutos()).isEqualTo(10);
    }

    @Test
    void reinicializarPreservaComissaoDoBarbeiro() throws Exception {
        var barbeiro = barbeiros.findByUsuario_Login("barbeiro@demo.com").orElseThrow();
        barbeiro.setPorcentagemComissao(35);
        barbeiros.saveAndFlush(barbeiro);

        bootstrap.run();

        assertThat(barbeiros.findById(barbeiro.getId()).orElseThrow().getPorcentagemComissao()).isEqualTo(35);
        assertThat(planos.count()).isEqualTo(3);
    }

    @Test
    void essencialTemHidratacaoComDescontoENovosServicosAvulsosSemDuplicar() throws Exception {
        var plano = planos.findByNomePlanoIgnoreCase("Plano Essencial").orElseThrow();
        var hidratacao = servicos.findByNomeIgnoreCase("Hidratacao").orElseThrow();
        var raspa = servicos.findByNomeIgnoreCase("Raspa na maquininha").orElseThrow();
        var acabamento = servicos.findByNomeIgnoreCase("Acabamento bigode + barba").orElseThrow();
        assertThat(raspa.getPreco()).isEqualByComparingTo("25.00");
        assertThat(raspa.getDuracaoMinutos()).isEqualTo(15);
        assertThat(acabamento.getPreco()).isEqualByComparingTo("20.00");
        assertThat(acabamento.getDuracaoMinutos()).isEqualTo(10);
        var regra = plano.getDescontosServicos().iterator().next();
        assertThat(regra.getPorcentagemDesconto()).isEqualTo(20);
        assertThat(regra.getServicos()).extracting(s -> s.getId()).containsExactly(hidratacao.getId());
        assertThat(plano.getServicosIncluidos()).extracting(s -> s.getId())
                .doesNotContain(hidratacao.getId(), raspa.getId(), acabamento.getId());
        assertThat(bruninho.Barbeiro.service.CalculoServicosPlano.calcular(hidratacao, plano).getPrecoLiquido())
                .isEqualByComparingTo("32.00");
        long quantidade = servicos.count();
        regra.setPorcentagemDesconto(25); planos.saveAndFlush(plano);
        bootstrap.run(); planos.flush();
        assertThat(plano.getDescontosServicos()).hasSize(1);
        assertThat(regra.getPorcentagemDesconto()).isEqualTo(25);
        assertThat(servicos.count()).isEqualTo(quantidade);
    }

    @Test
    void loginDoBarbeiroDemoFuncionaComEmailESenha() throws Exception {
        var login = mvc.perform(post("/login").with(csrf())
                        .param("username", "barbeiro@demo.com").param("password", "barbeiro123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value("/barbeiro/home")).andReturn();
        var sessao = (org.springframework.mock.web.MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/barbeiro/home").session(sessao))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/client/index.html"));
    }

    @Test
    void migraLoginAntigoSemDuplicarBarbeiroNemAlterarSenha() throws Exception {
        var barbeiro = barbeiros.findByUsuario_Login("barbeiro@demo.com").orElseThrow();
        var usuario = barbeiro.getUsuario();
        var usuarioId = usuario.getId();
        var senha = usuario.getSenha();
        usuario.setLogin("barbeiro");
        usuarios.saveAndFlush(usuario);

        bootstrap.run();

        var atualizado = barbeiros.findByUsuario_Login("barbeiro@demo.com").orElseThrow();
        assertThat(atualizado.getId()).isEqualTo(barbeiro.getId());
        assertThat(atualizado.getUsuario().getId()).isEqualTo(usuarioId);
        assertThat(atualizado.getUsuario().getSenha()).isEqualTo(senha);
        assertThat(usuarios.findByLogin("barbeiro")).isEmpty();
        assertThat(barbeiros.count()).isEqualTo(1);
        loginDoBarbeiroDemoFuncionaComEmailESenha();
    }
}
