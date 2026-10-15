package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class HomeBarbeiroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired AgendamentoRepository agendamentos;
    private Barbeiro barbeiro;

    @BeforeEach
    void preparar() {
        barbeiro = criarBarbeiro("home-barbeiro");
    }

    @Test
    void homeVaziaRetornaContadoresZeradosEListaVazia() throws Exception {
        mvc.perform(get("/api/barbeiro/home").with(user("home-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barbeiro.nome").value("home-barbeiro"))
                .andExpect(jsonPath("$.agendamentosHoje").value(0))
                .andExpect(jsonPath("$.concluidosHoje").value(0))
                .andExpect(jsonPath("$.canceladosHoje").value(0))
                .andExpect(jsonPath("$.proximosAgendamentos").isEmpty());
    }

    @Test
    void indicadoresContamSomenteODiaEBarbeiroAutenticado() throws Exception {
        var hoje = LocalDate.now();
        salvar(barbeiro, hoje, LocalTime.MIDNIGHT, StatusAgendamento.AGENDADO);
        salvar(barbeiro, hoje, LocalTime.MIDNIGHT, StatusAgendamento.CONCLUIDO);
        salvar(barbeiro, hoje, LocalTime.MIDNIGHT, StatusAgendamento.CANCELADO);
        salvar(barbeiro, hoje.minusDays(1), LocalTime.NOON, StatusAgendamento.CONCLUIDO);
        salvar(criarBarbeiro("outro-barbeiro"), hoje, LocalTime.NOON, StatusAgendamento.CANCELADO);

        mvc.perform(get("/api/barbeiro/home").with(user("home-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentosHoje").value(3))
                .andExpect(jsonPath("$.concluidosHoje").value(1))
                .andExpect(jsonPath("$.canceladosHoje").value(1));
    }

    @Test
    void proximosRespeitamHorarioStatusDiaOrdemELimite() {
        var dia = LocalDate.of(2026, 10, 14);
        salvar(barbeiro, dia, LocalTime.of(13, 59), StatusAgendamento.AGENDADO);
        salvar(barbeiro, dia, LocalTime.of(14, 10), StatusAgendamento.CANCELADO);
        salvar(barbeiro, dia, LocalTime.of(14, 20), StatusAgendamento.CONCLUIDO);
        salvar(barbeiro, dia, LocalTime.of(14, 30), StatusAgendamento.NAO_COMPARECEU);
        salvar(barbeiro, dia.plusDays(1), LocalTime.of(14, 0), StatusAgendamento.AGENDADO);
        salvar(criarBarbeiro("outro-barbeiro"), dia, LocalTime.of(14, 0), StatusAgendamento.AGENDADO);
        salvar(barbeiro, dia, LocalTime.of(17, 0), StatusAgendamento.AGENDADO);
        var terceiro = salvar(barbeiro, dia, LocalTime.of(16, 0), StatusAgendamento.AGENDADO);
        var segundo = salvar(barbeiro, dia, LocalTime.of(15, 0), StatusAgendamento.AGENDADO);
        var primeiro = salvar(barbeiro, dia, LocalTime.of(14, 0), StatusAgendamento.AGENDADO);

        var proximos = agendamentos.findProximosDoBarbeiroNoDia(barbeiro.getId(), dia,
                LocalTime.of(14, 0), StatusAgendamento.AGENDADO, PageRequest.of(0, 3));
        assertThat(proximos).extracting(Agendamento::getId)
                .containsExactly(primeiro.getId(), segundo.getId(), terceiro.getId());
    }

    @Test
    void clienteNaoPodeConsultarHomeDoBarbeiro() throws Exception {
        mvc.perform(get("/api/barbeiro/home").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioSemCadastroDeBarbeiroRecebe404() throws Exception {
        mvc.perform(get("/api/barbeiro/home").with(user("inexistente").roles("BARBEIRO")))
                .andExpect(status().isNotFound());
    }

    private Barbeiro criarBarbeiro(String login) {
        var usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha("senha-teste");
        usuario.setRole(ROLE.BARBEIRO);
        usuario.setAtivo(true);
        var novo = new Barbeiro();
        novo.setUsuario(usuarios.save(usuario));
        novo.setNome(login);
        novo.setTelefone("11999991111");
        return barbeiros.save(novo);
    }

    private Agendamento salvar(Barbeiro dono, LocalDate dia, LocalTime hora, StatusAgendamento status) {
        var novo = new Agendamento();
        novo.setBarbeiro(dono);
        novo.setData(dia);
        novo.setHoraInicio(hora);
        novo.setHoraFinalizacao(hora.plusMinutes(30));
        novo.setDuracaoServicoMinutos(30);
        novo.setNomeCliente("Cliente avulso");
        novo.setNomeServicoPlano("Corte");
        novo.setStatus(status);
        return agendamentos.save(novo);
    }
}
