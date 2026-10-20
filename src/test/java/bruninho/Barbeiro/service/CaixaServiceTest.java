package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.caixa.AbrirCaixaRequest;
import bruninho.Barbeiro.domain.Caixa;
import bruninho.Barbeiro.domain.StatusCaixa;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.CaixaRepository;
import bruninho.Barbeiro.repository.MovimentoCaixaRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaixaServiceTest {
    @Test
    void aberturaPreencheCamposObrigatoriosParaPersistencia() {
        var caixas = mock(CaixaRepository.class);
        when(caixas.findAberto()).thenReturn(Optional.empty());
        when(caixas.save(any(Caixa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        var service = new CaixaService(caixas, mock(MovimentoCaixaRepository.class), mock(BarbeiroRepository.class));

        var caixa = service.abrir(new AbrirCaixaRequest(new BigDecimal("200.00")));

        assertEquals(StatusCaixa.ABERTO, caixa.getStatus());
        assertEquals(new BigDecimal("200.00"), caixa.getSaldoInicial());
        assertEquals(BigDecimal.ZERO, caixa.getSaldoFinal());
        assertNotNull(caixa.getAbertoEm());
        verify(caixas).save(caixa);
    }
}
