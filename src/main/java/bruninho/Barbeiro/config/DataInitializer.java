package bruninho.Barbeiro.config;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.security.model.Usuario;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import bruninho.Barbeiro.security.model.ROLE;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.DayOfWeek;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DataInitializer {
    @Bean
    CommandLineRunner bootstrap(AppProperties props, UsuarioRepository usuarios,
                                ServicoRepository servicos, BarbeiroRepository barbeiros,
                                JornadaBarbeiroRepository jornadas,
                                ConfiguracaoBarbeariaRepository configuracoes,
                                HorarioFuncionamentoRepository horariosFuncionamento) {
        return args -> run(props, usuarios, servicos, barbeiros, jornadas, configuracoes, horariosFuncionamento);
    }

    @Transactional
    void run(AppProperties props, UsuarioRepository usuarios,
             ServicoRepository servicos, BarbeiroRepository barbeiros,
             JornadaBarbeiroRepository jornadas,
             ConfiguracaoBarbeariaRepository configuracoes,
             HorarioFuncionamentoRepository horariosFuncionamento) {
        ensureConfig(configuracoes);
        ensureHorarioFuncionamento(horariosFuncionamento);
        if (!props.getInitialAdmin().getLogin().isBlank() && !props.getInitialAdmin().getPassword().isBlank()) {
            criarUsuarioSeAusente(usuarios, props.getInitialAdmin().getLogin(),
                    props.getInitialAdmin().getPassword(), ROLE.ADMIN);
        }
        if (props.isDemoData() && servicos.count() == 0) {
            createDemoData(usuarios, servicos, barbeiros, jornadas);
        }
    }

    private void ensureConfig(ConfiguracaoBarbeariaRepository configuracoes) {
        configuracoes.findById(1L).orElseGet(() -> {
            ConfiguracaoBarbearia config = new ConfiguracaoBarbearia();
            config.setNome("Barbearia");
            config.setTelefone("");
            config.setEndereco("");
            config.setDiasMaximoAntecedentia(60);
            return configuracoes.save(config);
        });
    }

    private void ensureHorarioFuncionamento(HorarioFuncionamentoRepository horariosFuncionamento) {
        for (int day = 1; day <= 7; day++) {
            final int currentDay = day;
            if (horariosFuncionamento.findByDiaSemana(DayOfWeek.of(currentDay)) == null) {
                HorarioFuncionamento hours = new HorarioFuncionamento();
                hours.setDiaSemana(DayOfWeek.of(currentDay));
                hours.setHoraAbertura(LocalTime.of(9, 0));
                hours.setHoraFechamento(currentDay == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
                hours.setFechado(currentDay == 7);
                hours.setIntervaloMinimoEntreAgendamentos(15);
                horariosFuncionamento.save(hours);
            }
        }
    }

    private void createDemoData(UsuarioRepository usuarios,
                                ServicoRepository servicos, BarbeiroRepository barbeiros,
                                JornadaBarbeiroRepository jornadas) {
        Servico corte = new Servico();
        corte.setNome("Corte masculino");
        corte.setPreco(new BigDecimal("45.00"));
        corte.setDuracaoMinutos(45);
        servicos.save(corte);

        Usuario usuario = criarUsuarioSeAusente(usuarios, "barbeiro", "barbeiro123", ROLE.BARBEIRO);

        Barbeiro barbeiro = new Barbeiro();
        barbeiro.setNome("Barbeiro Demo");
        barbeiro.setTelefone("(11) 99999-0000");
        barbeiro.setUsuario(usuario);
        barbeiros.save(barbeiro);

        for (int day = 1; day <= 6; day++) {
            JornadaBarbeiro jornada = new JornadaBarbeiro();
            jornada.setBarbeiro(barbeiro);
            jornada.setDiaSemana(DayOfWeek.of(day));
            jornada.setAtivo(true);
            jornada.setFolga(false);
            jornada.setHoraInicio(LocalTime.of(9, 0));
            jornada.setHoraFim(day == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            jornada.setIntervaloInicio(LocalTime.of(12, 0));
            jornada.setIntervaloFim(LocalTime.of(13, 0));
            jornadas.save(jornada);
        }
    }

    private Usuario criarUsuarioSeAusente(UsuarioRepository usuarios, String login, String senha, ROLE role) {
        return usuarios.findByLogin(login).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setLogin(login);
            usuario.setSenha(new BCryptPasswordEncoder().encode(senha));
            usuario.setRole(role);
            usuario.setAtivo(true);
            return usuarios.save(usuario);
        });
    }
}
