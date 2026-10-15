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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class FinalizacaoAgendamentoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired ComissaoBarbeiroRepository comissoes;
    private Agendamento agendamento;

    @BeforeEach
    void preparar() {
        var usuario = new Usuario();
        usuario.setLogin("finalizar-barbeiro");
        usuario.setSenha("senha-teste");
        usuario.setRole(ROLE.BARBEIRO);
        usuario.setAtivo(true);
        var barbeiro = new Barbeiro();
        barbeiro.setUsuario(usuarios.save(usuario));
        barbeiro.setNome("Barbeiro");
        barbeiro.setTelefone("11999991111");
        barbeiro.setPorcentagemComissao(40);
        agendamento = new Agendamento();
        agendamento.setBarbeiro(barbeiros.save(barbeiro));
        agendamento.setNomeCliente("Cliente avulso");
        agendamento.setData(LocalDate.now().minusDays(1));
        agendamento.setHoraInicio(LocalTime.of(10, 0));
        agendamento.setHoraFinalizacao(LocalTime.of(10, 40));
        agendamento.setDuracaoServicoMinutos(40);
        agendamento.setPrecoTotal(new BigDecimal("70.00"));
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento = agendamentos.save(agendamento);
    }

    private String endpoint() {
        return "/api/barbeiro/agendamentos/" + agendamento.getId() + "/finalizacao";
    }

    @Test
    void consultaNaoMudaEstadoEPostPersistePagamentoComissaoEConclusao() throws Exception {
        mvc.perform(get(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podeFinalizar").value(true))
                .andExpect(jsonPath("$.valorCobrar").value(70))
                .andExpect(jsonPath("$.comissao.valorComissao").value(28));
        assertThat(comissoes.existsByAgendamento_Id(agendamento.getId())).isFalse();
        mvc.perform(post(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"))
                .andExpect(jsonPath("$.formaPagamento").value("PIX"))
                .andExpect(jsonPath("$.finalizadoEm").isNotEmpty());
        agendamentos.flush();
        assertThat(agendamento.getPagamentoRecebido()).isTrue();
        assertThat(agendamento.getFormaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(comissoes.existsByAgendamento_Id(agendamento.getId())).isTrue();
        mvc.perform(post(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void exigeFormaDePagamentoValida() throws Exception {
        for (var corpo : new String[] {"{}", "{\"formaPagamento\":\"INVALIDA\"}"}) {
            mvc.perform(post(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO"))
                            .contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").isNotEmpty());
        }
        assertThat(comissoes.existsByAgendamento_Id(agendamento.getId())).isFalse();
    }

    @Test
    void naoPermiteFinalizarAgendamentoFuturo() throws Exception {
        agendamento.setData(LocalDate.now().plusDays(1));
        mvc.perform(post(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"DINHEIRO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Agendamento so pode ser finalizado apos o horario de termino"));
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
        assertThat(comissoes.existsByAgendamento_Id(agendamento.getId())).isFalse();
    }

    @Test
    void mensalSemPlanoNaoGeraComissao() throws Exception {
        mvc.perform(post(endpoint()).with(user("finalizar-barbeiro").roles("BARBEIRO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"MENSAL\"}"))
                .andExpect(status().isBadRequest());
        assertThat(comissoes.existsByAgendamento_Id(agendamento.getId())).isFalse();
    }

    @Test
    void protegeConsultaEFinalizacaoContraOutroBarbeiroECliente() throws Exception {
        mvc.perform(get(endpoint()).with(user("outro").roles("BARBEIRO")))
                .andExpect(status().isNotFound());
        mvc.perform(post(endpoint()).with(user("outro").roles("BARBEIRO"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(get(endpoint()).with(user("cliente").roles("CLIENTE")))
                .andExpect(status().isForbidden());
        mvc.perform(post(endpoint()).with(user("cliente").roles("CLIENTE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"formaPagamento\":\"PIX\"}"))
                .andExpect(status().isForbidden());
    }
}
