package bruninho.Barbeiro.config;
import bruninho.Barbeiro.service.GerenciamentoPlanosService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration
@EnableScheduling
public class BloqueioPlanosScheduler {
    private final GerenciamentoPlanosService service;
    public BloqueioPlanosScheduler(GerenciamentoPlanosService service) { this.service = service; }
    @Scheduled(cron = "0 1 0 * * *", zone = "${app.zone:America/Sao_Paulo}")
    public void atualizar() { service.atualizarBloqueios(); }
}
