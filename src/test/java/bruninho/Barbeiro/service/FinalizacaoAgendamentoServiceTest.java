package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.*;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FinalizacaoAgendamentoServiceTest {
    private final AgendamentoRepository agendamentos = mock(AgendamentoRepository.class);
    private final ComissaoBarbeiroRepository comissoes = mock(ComissaoBarbeiroRepository.class);
    private final PlanoMensalClienteRepository planos = mock(PlanoMensalClienteRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T18:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final FinalizacaoAgendamentoService service = new FinalizacaoAgendamentoService(agendamentos, comissoes, planos, clock);
    private Agendamento agendamento;

    @BeforeEach
    void preparar() {
        var barbeiro = new Barbeiro();
        barbeiro.setId(10L);
        barbeiro.setPorcentagemComissao(35);
        var corte = new Servico();
        corte.setId(2L);
        corte.setNome("Corte");
        corte.setPreco(new BigDecimal("70.00"));
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setNomeCliente("Cliente avulso");
        agendamento.setData(LocalDate.of(2026, 10, 15));
        agendamento.setHoraInicio(LocalTime.of(14, 0));
        agendamento.setHoraFinalizacao(LocalTime.of(14, 40));
        agendamento.setDuracaoServicoMinutos(40);
        agendamento.setPrecoTotal(new BigDecimal("70.00"));
        agendamento.setStatus(StatusAgendamento.AGENDADO);
        agendamento.setServico(Set.of(corte));
        when(agendamentos.findParaFinalizar(1L, "barbeiro")).thenReturn(Optional.of(agendamento));
        when(agendamentos.findByIdAndBarbeiro_Usuario_Login(1L, "barbeiro")).thenReturn(Optional.of(agendamento));
    }

    @Test
    void calculaComissaoPadraoEFinalizaComHorarioDoServidor() {
        var resposta = service.finalizar("barbeiro", 1L, FormaPagamento.PIX);
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.PIX);
        assertThat(resposta.finalizadoEm()).isEqualTo(LocalDateTime.of(2026, 10, 15, 15, 0));
        assertThat(resposta.comissao().valorComissao()).isEqualByComparingTo("24.50");
        assertThat(agendamento.getPagamentoRecebido()).isTrue();
        var captor = ArgumentCaptor.forClass(ComissaoBarbeiro.class);
        verify(comissoes).save(captor.capture());
        assertThat(captor.getValue().getTipo()).isEqualTo(TipoComissao.PADRAO_BARBEIRO);
        assertThat(captor.getValue().getPercentualAplicado()).isEqualByComparingTo("35");
        assertThat(captor.getValue().getValorBase()).isEqualByComparingTo("70.00");
    }

    @Test
    void consultaAntesDoTerminoMostraBloqueioSemGravar() {
        agendamento.setHoraFinalizacao(LocalTime.of(15, 1));
        var resposta = service.consultar("barbeiro", 1L, null);
        assertThat(resposta.podeFinalizar()).isFalse();
        assertThat(resposta.motivoBloqueio()).isNotBlank();
        verifyNoInteractions(comissoes);
    }

    @Test
    void bloqueiaAntesENoInstanteExatoDoTermino() {
        for (var termino : new LocalTime[] {LocalTime.of(15, 1), LocalTime.of(15, 0)}) {
            agendamento.setHoraFinalizacao(termino);
            assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.DINHEIRO))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("apos o horario");
        }
        verify(comissoes, never()).save(any());
        assertThat(agendamento.getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }

    @Test
    void bloqueiaAgendamentoEmDataFutura() {
        agendamento.setData(agendamento.getData().plusDays(1));
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.PIX))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("apos o horario");
    }

    @Test
    void naoFinalizaCanceladoFaltaOuConcluido() {
        for (var status : new StatusAgendamento[] {StatusAgendamento.CANCELADO, StatusAgendamento.NAO_COMPARECEU, StatusAgendamento.CONCLUIDO}) {
            agendamento.setStatus(status);
            assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.PIX))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("AGENDADO");
        }
        verify(comissoes, never()).save(any());
    }

    @Test
    void bloqueiaRepeticaoDaFinalizacaoEDaComissao() {
        service.finalizar("barbeiro", 1L, FormaPagamento.DEBITO);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.DEBITO))
                .isInstanceOf(ResponseStatusException.class);
        verify(comissoes, times(1)).save(any());
    }

    @Test
    void comissaoExistenteImpedeNovaGeracao() {
        when(comissoes.existsByAgendamento_Id(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.CREDITO))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ja possui comissao");
        verify(comissoes, never()).save(any());
    }

    @Test
    void naoPermiteFinalizarAgendamentoDeOutroBarbeiro() {
        assertThatThrownBy(() -> service.finalizar("outro", 1L, FormaPagamento.PIX))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(comissoes);
    }

    @Test
    void exigePagamentoEPercentualValido() {
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, null))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("forma de pagamento");
        agendamento.getBarbeiro().setPorcentagemComissao(null);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.PIX))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Percentual");
        agendamento.getBarbeiro().setPorcentagemComissao(101);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.PIX))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("Percentual");
    }

    @Test
    void mensalExigeClienteEPlanoReal() {
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("cliente cadastrado");
        var cliente = new Cliente();
        cliente.setId(5L);
        agendamento.setCliente(cliente);
        when(planos.findByCliente_IdAndAtivoTrue(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("nao possui plano");
    }
}
