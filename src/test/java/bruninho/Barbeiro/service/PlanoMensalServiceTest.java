package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.requests.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlanoMensalServiceTest {
    private final PlanoMensalRepository planos = mock(PlanoMensalRepository.class);
    private final ServicoRepository servicos = mock(ServicoRepository.class);
    private final PlanoMensalClienteRepository contratos = mock(PlanoMensalClienteRepository.class);
    private final PlanoMensalService service = new PlanoMensalService(planos, servicos, contratos);

    private CriarPlanoMensal request() {
        var incluido = new Servico(); incluido.setId(1L);
        var extra = new Servico(); extra.setId(2L);
        when(servicos.findAllById(Set.of(1L))).thenReturn(List.of(incluido));
        when(servicos.findAllById(Set.of(2L))).thenReturn(List.of(extra));
        var request = new CriarPlanoMensal(); request.setNomePlano("Plano"); request.setDescricao("Corte");
        request.setValorMensal(BigDecimal.TEN); request.setServicosId(Set.of(1L));
        request.setDescontosServicos(List.of(new DescontoServicoPlanoRequest(20, Set.of(2L))));
        return request;
    }

    @Test
    void cadastraDescontoApenasParaOsExtrasConfigurados() {
        service.CadastrarPlanosMensais(request());
        var captor = ArgumentCaptor.forClass(PlanosMensal.class); verify(planos).save(captor.capture());
        var plano = captor.getValue();
        assertThat(plano.getServicosIncluidos()).extracting(Servico::getId).containsExactly(1L);
        var regra = plano.getDescontosServicos().iterator().next();
        assertThat(regra.getPlanoMensal()).isSameAs(plano);
        assertThat(regra.getPorcentagemDesconto()).isEqualTo(20);
        assertThat(regra.getServicos()).extracting(Servico::getId).containsExactly(2L);
    }

    @Test
    void rejeitaDescontoInvalidoEServicoEmDuasRegras() {
        var request = request();
        request.setDescontosServicos(List.of(new DescontoServicoPlanoRequest(101, Set.of(2L))));
        assertThatThrownBy(() -> service.CadastrarPlanosMensais(request)).hasMessageContaining("entre 0 e 100");
        request.setDescontosServicos(List.of(new DescontoServicoPlanoRequest(10, Set.of(2L)),
                new DescontoServicoPlanoRequest(20, Set.of(2L))));
        assertThatThrownBy(() -> service.CadastrarPlanosMensais(request)).hasMessageContaining("mais de uma regra");
        verify(planos, never()).save(any());
    }

    @Test
    void atualizacaoSemCampoPreservaDescontosEListaVaziaRemove() {
        request();
        var plano = new PlanosMensal(); plano.setId(1L);
        var regra = new ServicosPlanosDesconto(); plano.getDescontosServicos().add(regra);
        when(planos.findById(1L)).thenReturn(Optional.of(plano));
        when(contratos.findAllByPlanoMensal_Id(1L)).thenReturn(List.of());
        var request = new AtualizarPlanoMensal(); request.setId(1L); request.setServicosId(Set.of(1L));
        service.AtualizarPlanoMensal(request);
        assertThat(plano.getDescontosServicos()).containsExactly(regra);
        request.setDescontosServicos(List.of()); service.AtualizarPlanoMensal(request);
        assertThat(plano.getDescontosServicos()).isEmpty();
    }
}
