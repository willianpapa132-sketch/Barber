package bruninho.Barbeiro.config;

import bruninho.Barbeiro.platform.TenantContext;
import bruninho.Barbeiro.platform.TenantRegistry;
import bruninho.Barbeiro.service.GerenciamentoPlanosService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableScheduling
public class BloqueioPlanosScheduler {
    private static final Logger log = LoggerFactory.getLogger(BloqueioPlanosScheduler.class);
    private final GerenciamentoPlanosService service;
    private final TenantRegistry empresas;
    public BloqueioPlanosScheduler(GerenciamentoPlanosService service, TenantRegistry empresas) {
        this.service = service; this.empresas = empresas;
    }
    @Scheduled(cron = "0 1 0 * * *", zone = "${app.zone:America/Sao_Paulo}")
    public void atualizar() {
        for (var empresa : empresas.all()) {
            if (!"ATIVA".equals(empresa.estado())) continue;
            try {
                TenantContext.setEmpresaId(empresa.id());
                // O proxy abre e encerra uma transacao para cada empresa.
                service.atualizarBloqueios();
            } catch (RuntimeException e) {
                log.error("Falha ao atualizar planos da empresa {}", empresa.id(), e);
            } finally { TenantContext.clear(); }
        }
    }
}
