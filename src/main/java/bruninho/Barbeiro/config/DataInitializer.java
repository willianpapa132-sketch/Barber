package bruninho.Barbeiro.config;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.ServicosPlanosDesconto;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepository;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepository;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.model.Usuario;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class DataInitializer {
    private static final String BARBEIRO_DEMO_LOGIN = "barbeiro@demo.com";
    private static final String CLIENTE_DEMO_LOGIN = "cliente";

    @Bean
    CommandLineRunner bootstrap(
            AppProperties props,
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            ServicoRepository servicos,
            BarbeiroRepository barbeiros,
            JornadaBarbeiroRepository jornadas,
            ConfiguracaoBarbeariaRepository configuracoes,
            HorarioFuncionamentoRepository horariosFuncionamento,
            PlanoMensalRepository planosMensais,
            PasswordEncoder passwordEncoder,
            TransactionTemplate transactionTemplate
    ) {
        return args -> transactionTemplate.executeWithoutResult(status -> run(
                        props,
                        usuarios,
                        clientes,
                        servicos,
                        barbeiros,
                        jornadas,
                        configuracoes,
                        horariosFuncionamento,
                        planosMensais,
                        passwordEncoder
                )
        );
    }

    void run(
            AppProperties props,
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            ServicoRepository servicos,
            BarbeiroRepository barbeiros,
            JornadaBarbeiroRepository jornadas,
            ConfiguracaoBarbeariaRepository configuracoes,
            HorarioFuncionamentoRepository horariosFuncionamento,
            PlanoMensalRepository planosMensais,
            PasswordEncoder passwordEncoder
    ) {
        ensureConfig(configuracoes);
        ensureHorarioFuncionamento(horariosFuncionamento);

        if (!props.getInitialAdmin().getLogin().isBlank() && !props.getInitialAdmin().getPassword().isBlank()) {
            criarUsuarioSeAusente(
                    usuarios,
                    props.getInitialAdmin().getLogin(),
                    props.getInitialAdmin().getPassword(),
                    ROLE.ADMIN,
                    passwordEncoder
            );
        }

        if (props.isDemoData()) {
            createDemoData(usuarios, clientes, servicos, barbeiros, jornadas, planosMensais, passwordEncoder);
        }
    }

    private void ensureConfig(ConfiguracaoBarbeariaRepository configuracoes) {
        ConfiguracaoBarbearia config = configuracoes.findById(1L).orElseGet(() -> {
            ConfiguracaoBarbearia novaConfig = new ConfiguracaoBarbearia();
            novaConfig.setNome("Barber Shop Demo");
            novaConfig.setTelefone("(11) 99999-0000");
            novaConfig.setEndereco("Rua Demo, 100");
            novaConfig.setDiasMaximoAntecedentia(30);
            return configuracoes.save(novaConfig);
        });

        if (config.getDiasMaximoAntecedentia() == null || config.getDiasMaximoAntecedentia() > 30) {
            config.setDiasMaximoAntecedentia(30);
            configuracoes.save(config);
        }
    }

    private void ensureHorarioFuncionamento(HorarioFuncionamentoRepository horariosFuncionamento) {
        for (int day = 1; day <= 7; day++) {
            DayOfWeek diaSemana = DayOfWeek.of(day);
            HorarioFuncionamento hours = horariosFuncionamento.findByDiaSemana(diaSemana);

            if (hours == null) {
                hours = new HorarioFuncionamento();
                hours.setDiaSemana(diaSemana);
            }

            hours.setHoraAbertura(LocalTime.of(9, 0));
            hours.setHoraFechamento(day == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            hours.setFechado(day == 7);
            hours.setPermiteAgendamentoPlano(day <= 6);
            hours.setIntervaloMinimoEntreAgendamentos(15);
            horariosFuncionamento.save(hours);
        }
    }

    private void createDemoData(
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            ServicoRepository servicos,
            BarbeiroRepository barbeiros,
            JornadaBarbeiroRepository jornadas,
            PlanoMensalRepository planosMensais,
            PasswordEncoder passwordEncoder
    ) {
        Servico corte = ensureServico(servicos, "Corte", "45.00", 15);
        Servico barba = ensureServico(servicos, "Barba", "35.00", 10);
        Servico sobrancelha = ensureServico(servicos, "Sobrancelha", "25.00", 15);
        Servico hidratacao = ensureServico(servicos, "Hidratacao", "40.00", 15);
        Servico pigmentacao = ensureServico(servicos, "Pigmentacao", "55.00", 15);
        ensureServico(servicos, "Raspa na maquininha", "25.00", 15);
        ensureServico(servicos, "Acabamento bigode + barba", "20.00", 10);
        servicos.findByNomeIgnoreCase("Bigode").ifPresent(bigode -> {
            bigode.setDuracaoMinutos(10);
            bigode.setAtualizadoEm(LocalDateTime.now());
            servicos.save(bigode);
        });

        Usuario usuarioBarbeiro = ensureUsuarioBarbeiroDemo(usuarios, passwordEncoder);
        Barbeiro barbeiro = ensureBarbeiro(barbeiros, usuarioBarbeiro);
        ensureJornadaBarbeiro(jornadas, barbeiro);

        Usuario usuarioCliente = criarUsuarioSeAusente(
                usuarios,
                CLIENTE_DEMO_LOGIN,
                "cliente123",
                ROLE.CLIENTE,
                passwordEncoder
        );
        ensureCliente(clientes, usuarioCliente);

        var essencial = ensurePlano(
                planosMensais,
                "Plano Essencial",
                "Cuidado mensal essencial para manter o visual em dia.",
                "59.90",
                15,
                Set.of(corte, barba, sobrancelha)
        );
        ensureDescontoHidratacao(planosMensais, essencial, hidratacao);
        ensurePlano(
                planosMensais,
                "Plano Completo",
                "Mais servicos e antecedencia para sua rotina.",
                "89.90",
                20,
                Set.of(corte, barba, sobrancelha, hidratacao)
        );
        ensurePlano(
                planosMensais,
                "Plano Premium",
                "O plano mais completo para todos os cuidados do mes.",
                "119.90",
                30,
                Set.of(corte, barba, sobrancelha, hidratacao, pigmentacao)
        );
    }

    private Servico ensureServico(ServicoRepository servicos, String nome, String preco, int duracaoMinutos) {
        Servico servico = servicos.findByNomeIgnoreCase(nome).orElseGet(Servico::new);
        servico.setNome(nome);
        servico.setPreco(new BigDecimal(preco));
        servico.setDuracaoMinutos(duracaoMinutos);
        servico.setAtivo(true);
        servico.setAtualizadoEm(LocalDateTime.now());
        if (servico.getCriadoEm() == null) {
            servico.setCriadoEm(LocalDateTime.now());
        }
        return servicos.save(servico);
    }

    private Barbeiro ensureBarbeiro(BarbeiroRepository barbeiros, Usuario usuario) {
        Barbeiro barbeiro = barbeiros.findByUsuario_Login(usuario.getLogin()).orElseGet(Barbeiro::new);
        barbeiro.setNome("Willian Silva");
        barbeiro.setTelefone("(41) 99187-8175");
        barbeiro.setAtivo(true);
        barbeiro.setUsuario(usuario);
        if (barbeiro.getPorcentagemComissao() == null || barbeiro.getPorcentagemComissao() == 0) {
            barbeiro.setPorcentagemComissao(40);
        }
        barbeiro.setImgurl("https://images.unsplash.com/photo-1628157588553-5eeea00af15c?auto=format&fit=crop&w=300&q=80");
        return barbeiros.save(barbeiro);
    }

    private Cliente ensureCliente(ClienteRepository clientes, Usuario usuario) {
        Cliente cliente = clientes.findByUsuario_Login(usuario.getLogin()).orElseGet(Cliente::new);
        cliente.setNome("Cliente Demo");
        cliente.setTelefone("(41) 99999-0000");
        cliente.setImgUrl("https://images.unsplash.com/photo-1618077360395-f3068be8e001?auto=format&fit=crop&w=300&q=80");
        cliente.setUsuario(usuario);
        cliente.setAtualizadoEm(LocalDateTime.now());
        if (cliente.getCriadoEm() == null) {
            cliente.setCriadoEm(LocalDateTime.now());
        }
        return clientes.save(cliente);
    }

    private void ensureJornadaBarbeiro(JornadaBarbeiroRepository jornadas, Barbeiro barbeiro) {
        for (int day = 1; day <= 6; day++) {
            DayOfWeek diaSemana = DayOfWeek.of(day);
            int currentDay = day;
            JornadaBarbeiro jornada = jornadas.findByDiaSemanaAndBarbeiro_id(diaSemana, barbeiro.getId())
                    .orElseGet(JornadaBarbeiro::new);

            jornada.setBarbeiro(barbeiro);
            jornada.setDiaSemana(diaSemana);
            jornada.setAtivo(true);
            jornada.setFolga(false);
            jornada.setHoraInicio(LocalTime.of(9, 0));
            jornada.setHoraFim(currentDay == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            jornada.setIntervaloInicio(LocalTime.of(12, 0));
            jornada.setIntervaloFim(LocalTime.of(13, 0));
            jornadas.save(jornada);
        }
    }

    private PlanosMensal ensurePlano(
            PlanoMensalRepository planosMensais,
            String nome,
            String descricao,
            String valor,
            int diasMaximoAntecedencia,
            Set<Servico> servicos
    ) {
        PlanosMensal plano = planosMensais.findByNomePlanoIgnoreCase(nome).orElseGet(PlanosMensal::new);
        plano.setNomePlano(nome);
        plano.setDescricao(descricao);
        plano.setValorMensal(new BigDecimal(valor));
        plano.setDiasMaximoAntecedencia(diasMaximoAntecedencia);
        plano.setAtivo(true);

        plano.getServicosIncluidos().clear();
        plano.getServicosIncluidos().addAll(servicos);
        plano.setAtualizadoEm(LocalDateTime.now());
        if (plano.getCriadoEm() == null) {
            plano.setCriadoEm(LocalDateTime.now());
        }
        return planosMensais.save(plano);
    }

    private void ensureDescontoHidratacao(PlanoMensalRepository planosMensais, PlanosMensal plano, Servico hidratacao) {
        boolean configurado = plano.getDescontosServicos().stream()
                .anyMatch(regra -> regra.getServicos().stream().anyMatch(s -> s.getId().equals(hidratacao.getId())));
        if (configurado) return;
        var desconto = new ServicosPlanosDesconto();
        desconto.setPlanoMensal(plano);
        desconto.setPorcentagemDesconto(20);
        desconto.setServicos(new java.util.HashSet<>(Set.of(hidratacao)));
        plano.getDescontosServicos().add(desconto);
        planosMensais.save(plano);
    }

    private Usuario ensureUsuarioBarbeiroDemo(UsuarioRepository usuarios, PasswordEncoder passwordEncoder) {
        var usuario = usuarios.findByLogin(BARBEIRO_DEMO_LOGIN)
                .or(() -> usuarios.findByLogin("barbeiro"));
        if (usuario.isPresent()) {
            var existente = usuario.get();
            if (existente.getRole() != ROLE.BARBEIRO) {
                throw new IllegalStateException("Login do barbeiro demo pertence a outro perfil");
            }
            existente.setLogin(BARBEIRO_DEMO_LOGIN);
            return usuarios.save(existente);
        }
        return criarUsuarioSeAusente(usuarios, BARBEIRO_DEMO_LOGIN, "barbeiro123", ROLE.BARBEIRO, passwordEncoder);
    }

    private Usuario criarUsuarioSeAusente(
            UsuarioRepository usuarios,
            String login,
            String senha,
            ROLE role,
            PasswordEncoder passwordEncoder
    ) {
        return usuarios.findByLogin(login).orElseGet(() -> {
            Usuario usuario = new Usuario();
            usuario.setLogin(login);
            usuario.setSenha(passwordEncoder.encode(senha));
            usuario.setRole(role);
            usuario.setAtivo(true);
            return usuarios.save(usuario);
        });
    }
}
