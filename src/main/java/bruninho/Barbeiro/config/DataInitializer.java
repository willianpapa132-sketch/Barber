package bruninho.Barbeiro.config;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.security.model.Usuario;
import bruninho.Barbeiro.security.service.UsuarioCadastroServico;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DataInitializer {
    @Bean
    CommandLineRunner bootstrap(AppProperties props, UsuarioCadastroServico usuarios,
                                ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
                                JornadaBarbeiroRepositorio jornadas,
                                ConfiguracaoBarbeariaRepositorio configuracoes,
                                HorarioFuncionamentoRepositorio horariosFuncionamento) {
        return args -> run(props, usuarios, servicos, barbeiros, jornadas, configuracoes, horariosFuncionamento);
    }

    @Transactional
    void run(AppProperties props, UsuarioCadastroServico usuarios,
             ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
             JornadaBarbeiroRepositorio jornadas,
             ConfiguracaoBarbeariaRepositorio configuracoes,
             HorarioFuncionamentoRepositorio horariosFuncionamento) {
        ensureConfig(configuracoes);
        ensureHorarioFuncionamento(horariosFuncionamento);
        usuarios.criarAdminInicialSeAusente(props.getInitialAdmin());
        if (props.isDemoData() && servicos.count() == 0) {
            createDemoData(usuarios, servicos, barbeiros, jornadas);
        }
    }

    private void ensureConfig(ConfiguracaoBarbeariaRepositorio configuracoes) {
        configuracoes.findById(1L).orElseGet(() -> {
            ConfiguracaoBarbearia config = new ConfiguracaoBarbearia();
            config.setNome("Barbearia");
            config.setTelefone("");
            config.setEndereco("");
            config.setMinAntecedenciaMinutos(30);
            config.setHorizonteDias(60);
            return configuracoes.save(config);
        });
    }

    private void ensureHorarioFuncionamento(HorarioFuncionamentoRepositorio horariosFuncionamento) {
        for (int day = 1; day <= 7; day++) {
            final int currentDay = day;
            horariosFuncionamento.findByDiaSemana(currentDay).orElseGet(() -> {
                HorarioFuncionamento hours = new HorarioFuncionamento();
                hours.setDiaSemana(currentDay);
                hours.setHoraAbertura(LocalTime.of(9, 0));
                hours.setHoraFechamento(currentDay == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
                hours.setFechado(currentDay == 7);
                return horariosFuncionamento.save(hours);
            });
        }
    }

    private void createDemoData(UsuarioCadastroServico usuarios,
                                ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
                                JornadaBarbeiroRepositorio jornadas) {
        Servico corte = new Servico();
        corte.setNome("Corte masculino");
        corte.setDescricao("Corte com acabamento");
        corte.setPreco(new BigDecimal("45.00"));
        corte.setDuracaoMinutos(45);
        servicos.save(corte);

        Usuario usuario = usuarios.criarBarbeiroDemo("barbeiro", "barbeiro123", "Barbeiro Demo");

        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setNome("Barbeiro Demo");
        barbeiro.setTelefone("(11) 99999-0000");
        barbeiro.setUsuario(usuario);
        barbeiro.getServicos().add(corte);
        barbeiros.save(barbeiro);

        for (int day = 1; day <= 6; day++) {
            JornadaBarbeiro jornada = new JornadaBarbeiro();
            jornada.setBarbeiro(barbeiro);
            jornada.setDiaSemana(day);
            jornada.setHoraInicio(LocalTime.of(9, 0));
            jornada.setHoraFim(day == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            jornada.setIntervaloInicio(LocalTime.of(12, 0));
            jornada.setIntervaloFim(LocalTime.of(13, 0));
            jornadas.save(jornada);
        }
    }
}
