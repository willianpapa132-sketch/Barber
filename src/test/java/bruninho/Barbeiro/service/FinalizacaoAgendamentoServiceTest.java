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
    private final ComissaoPlanoRepository comissoesPlano = mock(ComissaoPlanoRepository.class);
    private final PlanoMensalClienteRepository planos = mock(PlanoMensalClienteRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-15T18:00:00Z"), ZoneId.of("America/Sao_Paulo"));
    private final FinalizacaoAgendamentoService service = new FinalizacaoAgendamentoService(agendamentos, comissoes, comissoesPlano, planos, clock);
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
    void mensalRegistraPlanoDataEPontosSemGerarComissaoEmReais() {
        var plano = vincularPlano();
        agendamento.getBarbeiro().setPorcentagemComissao(null);
        agendamento.setPrecoTotal(BigDecimal.ZERO);
        var resposta = service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL);
        assertThat(resposta.status()).isEqualTo(StatusAgendamento.CONCLUIDO);
        assertThat(resposta.comissao()).isNull();
        assertThat(resposta.valorCobrar()).isZero();
        assertThat(resposta.formaPagamento()).isEqualTo(FormaPagamento.MENSAL);

        var captor = ArgumentCaptor.forClass(ComissaoPlano.class);
        verify(comissoesPlano).save(captor.capture());
        var registro = captor.getValue();
        assertThat(registro.getPlanoMensal()).isSameAs(plano.getPlanoMensal());
        assertThat(registro.getPlanoMensalCliente()).isSameAs(plano);
        assertThat(registro.getAgendamento()).isSameAs(agendamento);
        assertThat(registro.getBarbeiro()).isSameAs(agendamento.getBarbeiro());
        assertThat(registro.getPontos()).isEqualTo(40);
        assertThat(resposta.comissaoPlano().pontos()).isEqualTo(40);
        assertThat(registro.getDataAgendamento()).isEqualTo(agendamento.getData());
        assertThat(registro.getCriadoEm()).isEqualTo(LocalDateTime.now(clock));

        verify(comissoes, never()).save(any());
    }

    @Test
    void previaMensalUsaPlanoMesmoSemPercentualNoBarbeiroENaoContabiliza() {
        vincularPlano();
        agendamento.getBarbeiro().setPorcentagemComissao(null);
        var resposta = service.consultar("barbeiro", 1L, null);
        assertThat(resposta.comissao()).isNull();
        assertThat(resposta.comissaoPlano().planoId()).isEqualTo(8L);
        assertThat(resposta.formasPagamentoDisponiveis()).containsExactly(FormaPagamento.MENSAL);
        verifyNoInteractions(comissoes, comissoesPlano);
    }

    @Test
    void bloqueiaPlanoInativoEPagamentoAvulsoEmMensal() {
        var plano = vincularPlano();
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.PIX))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("pagamento MENSAL");

        plano.setAtivo(false);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("inativo");
        verify(comissoesPlano, never()).save(any());
    }

    @Test
    void registroDePlanoExistenteImpedeDuplicacao() {
        vincularPlano();
        when(comissoesPlano.existsByAgendamento_Id(1L)).thenReturn(true);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("ja possui comissao");
        verify(comissoesPlano, never()).save(any());
    }

    @Test
    void planoDeMultiplosServicosGeraSessentaPontosUmaUnicaVez() {
        var bigode = new Servico();
        bigode.setId(3L);
        bigode.setNome("Bigode");
        bigode.setPreco(BigDecimal.TEN);
        bigode.setDuracaoMinutos(20);
        agendamento.getServico().iterator().next().setDuracaoMinutos(40);
        agendamento.setServico(Set.of(agendamento.getServico().iterator().next(), bigode));
        agendamento.setDuracaoServicoMinutos(60);
        vincularPlano();
        assertThat(service.consultar("barbeiro", 1L, null).comissaoPlano().pontos()).isEqualTo(60);
        verify(comissoesPlano, never()).save(any());
        // Uma alteracao posterior do catalogo nao muda a duracao contratada.
        bigode.setDuracaoMinutos(10);
        assertThat(service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL).comissaoPlano().pontos()).isEqualTo(60);
        var captor = ArgumentCaptor.forClass(ComissaoPlano.class);
        verify(comissoesPlano).save(captor.capture());
        assertThat(captor.getValue().getPontos()).isEqualTo(60);
        assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                .isInstanceOf(ResponseStatusException.class);
        verify(comissoesPlano, times(1)).save(any());
    }

    @Test
    void planoSemDuracaoValidaNaoGeraPontos() {
        vincularPlano();
        for (int minutos : new int[] {0, -10}) {
            agendamento.setDuracaoServicoMinutos(minutos);
            assertThatThrownBy(() -> service.finalizar("barbeiro", 1L, FormaPagamento.MENSAL))
                    .isInstanceOf(ResponseStatusException.class).hasMessageContaining("duracao valida");
        }
        verify(comissoesPlano, never()).save(any());
    }

    private PlanoMensalCliente vincularPlano() {
        var cliente = new Cliente();
        cliente.setId(5L);
        agendamento.setCliente(cliente);
        var plano = new PlanosMensal();
        plano.setId(8L);
        plano.setNomePlano("Plano Corte");

        plano.setAtivo(true);
        plano.setServicosIncluidos(agendamento.getServico());
        var contrato = new PlanoMensalCliente();
        contrato.setId(9L);
        contrato.setCliente(cliente);
        contrato.setPlanoMensal(plano);
        contrato.setAtivo(true);
        agendamento.setPlanoMensalCliente(contrato);
        return contrato;
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
