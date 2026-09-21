package bruninho.Barbeiro.config;

import bruninho.Barbeiro.domain.Usuario;
import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Perfil;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.repository.UsuarioRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepositorio;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DataInitializer {
    @Bean
    CommandLineRunner bootstrap(AppProperties props, UsuarioRepositorio usuarios, PasswordEncoder encoder,
                                ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
                                JornadaBarbeiroRepositorio jornadas,
                                ConfiguracaoBarbeariaRepositorio configuracoes,
                                HorarioFuncionamentoRepositorio horariosFuncionamento) {
        return args -> run(props, usuarios, encoder, servicos, barbeiros, jornadas, configuracoes, horariosFuncionamento);
    }

    @Transactional
    void run(AppProperties props, UsuarioRepositorio usuarios, PasswordEncoder encoder,
             ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
             JornadaBarbeiroRepositorio jornadas,
             ConfiguracaoBarbeariaRepositorio configuracoes,
             HorarioFuncionamentoRepositorio horariosFuncionamento) {
        ensureConfig(configuracoes);
        ensureHorarioFuncionamento(horariosFuncionamento);
        ensureInitialAdmin(props, usuarios, encoder);
        if (props.isDemoData() && servicos.count() == 0) {
            createDemoData(usuarios, encoder, servicos, barbeiros, jornadas);
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

    private void ensureInitialAdmin(AppProperties props, UsuarioRepositorio usuarios, PasswordEncoder encoder) {
        var admin = props.getInitialAdmin();
        if (!admin.getLogin().isBlank() && !usuarios.existsByLogin(admin.getLogin())) {
            Usuario usuario = new Usuario();
            usuario.setLogin(admin.getLogin());
            usuario.setSenhaHash(encoder.encode(admin.getPassword()));
            usuario.setNome(admin.getName());
            usuario.setPerfil(Perfil.ADMIN);
            usuarios.save(usuario);
        }
    }

    private void createDemoData(UsuarioRepositorio usuarios, PasswordEncoder encoder,
                                ServicoRepositorio servicos, BarbeiroRepositorio barbeiros,
                                JornadaBarbeiroRepositorio jornadas) {
        Servico corte = new Servico();
        corte.setNome("Corte masculino");
        corte.setDescricao("Corte com acabamento");
        corte.setPreco(new BigDecimal("45.00"));
        corte.setDuracaoMinutos(45);
        servicos.save(corte);

        Usuario usuario = new Usuario();
        usuario.setLogin("barbeiro");
        usuario.setSenhaHash(encoder.encode("barbeiro123"));
        usuario.setNome("Barbeiro Demo");
        usuario.setPerfil(Perfil.BARBEIRO);
        usuarios.save(usuario);

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
