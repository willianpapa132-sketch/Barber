package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepository;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CobrancaPlanoServiceTest {
    private CobrancaPlanoService service(String hoje, int dias) {
        var configs = mock(ConfiguracaoBarbeariaRepository.class);
        var config = new ConfiguracaoBarbearia(); config.setDiasToleranciaPagamentoPlano(dias);
        when(configs.findById(1L)).thenReturn(Optional.of(config));
        return new CobrancaPlanoService(configs, Clock.fixed(Instant.parse(hoje + "T12:00:00Z"), ZoneId.of("America/Sao_Paulo")));
    }
    private PlanoMensalCliente contrato(String inicio, String vencimento) {
        var p = new PlanoMensalCliente(); p.setAtivo(true); p.setCriadoEm(LocalDateTime.parse(inicio + "T10:00:00"));
        p.setProximoVencimento(LocalDate.parse(vencimento)); return p;
    }
    @Test void toleranciaIncluiUltimoDiaEBloqueiaNoSeguinte() {
        var p = contrato("2026-01-15", "2026-10-15");
        assertFalse(service("2026-10-18", 3).bloqueado(p));
        assertTrue(service("2026-10-19", 3).bloqueado(p));
        assertFalse(service("2026-10-15", 0).bloqueado(p));
        assertTrue(service("2026-10-16", 0).bloqueado(p));
    }
    @Test void fimDeMesPreservaDiaOriginalInclusiveAnoBissexto() {
        var p = contrato("2024-01-31", "2024-01-31");
        var service = service("2024-01-31", 0);
        p.setProximoVencimento(service.proximo(p));
        assertEquals(LocalDate.of(2024, 2, 29), p.getProximoVencimento());
        assertEquals(LocalDate.of(2024, 3, 31), service.proximo(p));
        p.setProximoVencimento(LocalDate.of(2025, 1, 31));
        assertEquals(LocalDate.of(2025, 2, 28), service.proximo(p));
    }
    @Test void liberacaoExpiraSemQuitarDividaNemProrrogarVencimento() {
        var p = contrato("2026-01-15", "2026-09-15"); p.setPagamentoAtrasado(true);
        var service = service("2026-10-20", 3);
        p.setLiberadoAte(service.fimLiberacao(p));
        assertEquals(LocalDate.of(2026, 11, 14), p.getLiberadoAte());
        assertFalse(service.bloqueado(p));
        assertFalse(service("2026-11-14", 3).bloqueado(p));
        assertTrue(service("2026-11-15", 3).bloqueado(p));
        assertEquals(LocalDate.of(2026, 9, 15), p.getProximoVencimento());
    }
    @Test void contratosLegadosUsamCriadoEmSemInventarPagamento() {
        var p = contrato("2026-01-15", "2026-01-15"); p.setProximoVencimento(null);
        assertEquals(LocalDate.of(2026, 1, 15), service("2026-10-20", 3).vencimento(p));
        assertTrue(service("2026-10-20", 3).bloqueado(p));
    }
}
