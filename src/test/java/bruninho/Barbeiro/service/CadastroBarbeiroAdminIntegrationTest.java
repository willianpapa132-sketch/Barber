package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.stream.Collectors;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CadastroBarbeiroAdminIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired JornadaBarbeiroRepository jornadas;
    @Autowired PasswordEncoder encoder;

    private String payload(String email, String senha, boolean intervaloInvalido) {
        var dias = Arrays.stream(DayOfWeek.values()).map(d -> """
                {"diaSemana":"%s","folga":%s,"horaInicio":"09:00","horaFim":"18:00",
                 "intervaloInicio":"%s","intervaloFim":"13:00"}
                """.formatted(d, d.getValue() >= 6, intervaloInvalido ? "19:00" : "12:00")).collect(Collectors.joining(","));
        return """
                {"nome":"Barbeiro Teste","telefone":"11999990000","login":"%s","senha":"%s",
                 "comissao":45,"ativo":true,"imgUrl":null,"jornada":[%s]}
                """.formatted(email, senha, dias);
    }

    @Test
    void criaDoisBarbeirosComMesmosDiasEEditaSemDuplicarUsuarioOuJornada() throws Exception {
        for (var email : new String[]{"primeiro@teste.com", "segundo@teste.com"}) {
            mvc.perform(post("/api/admin/barbeiros").with(user("admin").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON).content(payload(email, "senha12345", false)))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.jornada.length()").value(7))
                    .andExpect(jsonPath("$.senha").doesNotExist());
        }
        var b = barbeiros.findByUsuario_Login("primeiro@teste.com").orElseThrow();
        assertThat(b.getUsuario().getRole()).isEqualTo(ROLE.BARBEIRO);
        assertThat(encoder.matches("senha12345", b.getUsuario().getSenha())).isTrue();
        var senha = b.getUsuario().getSenha(); var quantidadeUsuarios = usuarios.count(); var quantidadeJornadas = jornadas.count();
        mvc.perform(put("/api/admin/barbeiros/" + b.getId()).with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(payload("primeiro@teste.com", "", false)))
                .andExpect(status().isOk());
        assertThat(usuarios.count()).isEqualTo(quantidadeUsuarios);
        assertThat(jornadas.count()).isEqualTo(quantidadeJornadas);
        assertThat(b.getUsuario().getSenha()).isEqualTo(senha);
        var folga = jornadas.findByDiaSemanaAndBarbeiro_id(DayOfWeek.SUNDAY, b.getId()).orElseThrow();
        assertThat(folga.getFolga()).isTrue(); assertThat(folga.getHoraInicio()).isNull();
    }

    @Test
    void rejeitaEmailDuplicadoEJornadaInvalidaSemCriarRegistros() throws Exception {
        var count = barbeiros.count(); var usuariosAntes = usuarios.count();
        mvc.perform(post("/api/admin/barbeiros").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(payload("invalido@teste.com", "senha12345", true)))
                .andExpect(status().isBadRequest());
        assertThat(barbeiros.count()).isEqualTo(count); assertThat(usuarios.count()).isEqualTo(usuariosAntes);
        mvc.perform(post("/api/admin/barbeiros").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(payload("duplicado@teste.com", "senha12345", false)))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/admin/barbeiros").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(payload("DUPLICADO@teste.com", "senha12345", false)))
                .andExpect(status().isBadRequest());
        assertThat(barbeiros.count()).isEqualTo(count + 1);
    }

    @Test
    void barbeiroNaoPodeCadastrarEquipe() throws Exception {
        mvc.perform(post("/api/admin/barbeiros").with(user("barbeiro").roles("BARBEIRO"))
                .contentType(MediaType.APPLICATION_JSON).content(payload("bloqueado@teste.com", "senha12345", false)))
                .andExpect(status().isForbidden());
    }
}
