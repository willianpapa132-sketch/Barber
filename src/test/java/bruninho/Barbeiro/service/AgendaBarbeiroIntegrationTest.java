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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class AgendaBarbeiroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired AgendamentoRepository agendamentos;
    private Barbeiro barbeiro;

    @BeforeEach
    void preparar() {
        barbeiro = criarBarbeiro("agenda-barbeiro");
    }

    @Test
    void padraoRetornaHojeIncluindoHorariosPassadosESomenteDoBarbeiroLogado() throws Exception {
        var hoje = LocalDate.now();
        salvar(barbeiro, hoje, 0, StatusAgendamento.CONCLUIDO);
        salvar(barbeiro, hoje, 23, StatusAgendamento.CANCELADO);
        salvar(barbeiro, hoje.plusDays(1), 10, StatusAgendamento.AGENDADO);
        salvar(criarBarbeiro("outro"), hoje, 10, StatusAgendamento.AGENDADO);

        mvc.perform(get("/api/barbeiro/agenda").with(user("agenda-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(hoje.toString()))
                .andExpect(jsonPath("$.totalAgendamentos").value(2))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.agendamentos[0].status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.agendamentos[1].status").value("CANCELADO"));
    }

    @Test
    void filtraDataEStatusComPaginacaoEOrdemPorHorario() throws Exception {
        var dia = LocalDate.of(2026, 10, 20);
        salvar(barbeiro, dia, 16, StatusAgendamento.AGENDADO);
        salvar(barbeiro, dia, 12, StatusAgendamento.CANCELADO);
        salvar(barbeiro, dia, 10, StatusAgendamento.AGENDADO);
        salvar(barbeiro, dia, 9, StatusAgendamento.AGENDADO);
        salvar(barbeiro, dia.minusDays(1), 8, StatusAgendamento.AGENDADO);

        mvc.perform(get("/api/barbeiro/agenda").param("data", dia.toString())
                        .param("status", "AGENDADO").param("tamanho", "2")
                        .with(user("agenda-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGENDADO"))
                .andExpect(jsonPath("$.totalAgendamentos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.temProximaPagina").value(true))
                .andExpect(jsonPath("$.agendamentos.length()").value(2))
                .andExpect(jsonPath("$.agendamentos[0].horaInicio").value("09:00:00"))
                .andExpect(jsonPath("$.agendamentos[0].servicos[0]").value("Corte"))
                .andExpect(jsonPath("$.agendamentos[0].nomeCliente").value("Cliente avulso"))
                .andExpect(jsonPath("$.agendamentos[0].duracaoMinutos").value(30))
                .andExpect(jsonPath("$.agendamentos[1].horaInicio").value("10:00:00"));

        mvc.perform(get("/api/barbeiro/agenda").param("data", dia.toString())
                        .param("status", "AGENDADO").param("tamanho", "2").param("pagina", "1")
                        .with(user("agenda-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.temProximaPagina").value(false))
                .andExpect(jsonPath("$.agendamentos.length()").value(1))
                .andExpect(jsonPath("$.agendamentos[0].horaInicio").value("16:00:00"));
    }

    @Test
    void diaSemAgendamentosRetornaListaVazia() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda").param("data", "2026-01-01")
                        .with(user("agenda-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agendamentos").isEmpty())
                .andExpect(jsonPath("$.totalAgendamentos").value(0))
                .andExpect(jsonPath("$.temProximaPagina").value(false));
    }

    @Test
    void rejeitaFiltrosEPaginacaoInvalidos() throws Exception {
        for (var parametro : new String[][] {
                {"data", "data-invalida"}, {"status", "PENDENTE"},
                {"pagina", "-1"}, {"tamanho", "0"}, {"tamanho", "101"}
        }) {
            mvc.perform(get("/api/barbeiro/agenda").param(parametro[0], parametro[1])
                            .with(user("agenda-barbeiro").roles("BARBEIRO")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void clienteNaoPodeAcessarAgendaDoBarbeiro() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda").with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void barbeiroSemCadastroRecebe404() throws Exception {
        mvc.perform(get("/api/barbeiro/agenda").with(user("inexistente").roles("BARBEIRO")))
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

    private void salvar(Barbeiro dono, LocalDate dia, int hora, StatusAgendamento status) {
        var novo = new Agendamento();
        novo.setBarbeiro(dono);
        novo.setData(dia);
        novo.setHoraInicio(LocalTime.of(hora, 0));
        novo.setHoraFinalizacao(LocalTime.of(hora, 30));
        novo.setDuracaoServicoMinutos(30);
        novo.setNomeCliente("Cliente avulso");
        novo.setNomeServicoPlano("Corte");
        novo.setStatus(status);
        agendamentos.save(novo);
    }
}
