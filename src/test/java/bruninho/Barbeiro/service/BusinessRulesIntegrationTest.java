package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.BloqueioBarbeiro;
import bruninho.Barbeiro.domain.ConfiguracaoBarbearia;
import bruninho.Barbeiro.domain.FormaPagamento;
import bruninho.Barbeiro.domain.HorarioFuncionamento;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Perfil;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.TipoMovimentoCaixa;
import bruninho.Barbeiro.domain.Usuario;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.BloqueioBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoPlanoMensalRepositorio;
import bruninho.Barbeiro.repository.HorarioFuncionamentoRepositorio;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepositorio;
import bruninho.Barbeiro.repository.MovimentoCaixaRepositorio;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.repository.SessaoCaixaRepositorio;
import bruninho.Barbeiro.repository.UsuarioRepositorio;
import bruninho.Barbeiro.web.form.AgendamentoPublicoForm;
import bruninho.Barbeiro.web.form.PublicPlanoMensalForm;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class BusinessRulesIntegrationTest {
    @Autowired ConfiguracaoBarbeariaRepositorio configs;
    @Autowired HorarioFuncionamentoRepositorio shopHours;
    @Autowired ServicoRepositorio servicos;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired BarbeiroRepositorio barbers;
    @Autowired JornadaBarbeiroRepositorio schedules;
    @Autowired BloqueioBarbeiroRepositorio blocks;
    @Autowired AgendamentoRepositorio agendamentos;
    @Autowired AgendamentoServico agendamentoService;
    @Autowired DisponibilidadeServico availability;
    @Autowired CaixaServico cashService;
    @Autowired MovimentoCaixaRepositorio movements;
    @Autowired SessaoCaixaRepositorio sessions;
    @Autowired PlanoMensalServico planoMensalService;
    @Autowired PlanoMensalClienteRepositorio planosMensais;
    @Autowired ConfiguracaoPlanoMensalRepositorio configuracoesPlanoMensal;

    Servico service;
    Barbeiro barber;
    LocalDate nextMonday;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        usuarios.findByLogin("admin").orElseGet(() -> {
            Usuario admin = new Usuario();
            admin.setLogin("admin");
            admin.setSenhaHash("hash");
            admin.setNome("Admin");
            admin.setPerfil(Perfil.ADMIN);
            return usuarios.save(admin);
        });
        movements.deleteAll();
        sessions.deleteAll();
        agendamentos.deleteAll();
        planosMensais.deleteAll();
        configuracoesPlanoMensal.deleteAll();
        blocks.deleteAll();
        schedules.deleteAll();
        barbers.deleteAll();
        servicos.deleteAll();
        configs.deleteAll();
        ConfiguracaoBarbearia config = new ConfiguracaoBarbearia();
        config.setNome("Barbearia Teste");
        config.setMinAntecedenciaMinutos(0);
        config.setHorizonteDias(60);
        configs.save(config);
        shopHours.deleteAll();
        for (int d = 1; d <= 7; d++) {
            HorarioFuncionamento h = new HorarioFuncionamento();
            h.setDiaSemana(d);
            h.setHoraAbertura(LocalTime.of(9, 0));
            h.setHoraFechamento(LocalTime.of(18, 0));
            h.setFechado(false);
            shopHours.save(h);
        }
        service = new Servico();
        service.setNome("Corte");
        service.setPreco(new BigDecimal("50.00"));
        service.setDuracaoMinutos(45);
        service = servicos.save(service);

        Usuario usuario = new Usuario();
        usuario.setLogin("barbeiro-rule-" + System.nanoTime());
        usuario.setSenhaHash("hash");
        usuario.setNome("Barbeiro");
        usuario.setPerfil(Perfil.BARBEIRO);
        usuarios.save(usuario);
        barber = new Barbeiro();
        barber.setNome("Barbeiro");
        barber.setUsuario(usuario);
        barber.getServicos().add(service);
        barber = barbers.save(barber);

        JornadaBarbeiro schedule = new JornadaBarbeiro();
        schedule.setBarbeiro(barber);
        schedule.setDiaSemana(1);
        schedule.setHoraInicio(LocalTime.of(9, 0));
        schedule.setHoraFim(LocalTime.of(17, 0));
        schedule.setIntervaloInicio(LocalTime.of(12, 0));
        schedule.setIntervaloFim(LocalTime.of(13, 0));
        schedules.save(schedule);

        int daysUntilMonday = (8 - LocalDate.now().getDayOfWeek().getValue()) % 7;
        nextMonday = LocalDate.now().plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
    }

    @Test
    void disponibilidadeRespeitaDuracaoJornadaIntervaloBloqueioESobreposicao() {
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).contains(LocalTime.of(9, 0));
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).doesNotContain(LocalTime.of(11, 30), LocalTime.of(16, 30));

        BloqueioBarbeiro block = new BloqueioBarbeiro();
        block.setBarbeiro(barber);
        block.setDataBloqueio(nextMonday);
        block.setHoraInicio(LocalTime.of(10, 0));
        block.setHoraFim(LocalTime.of(11, 0));
        blocks.save(block);
        assertThat(availability.availableSlots(service.getId(), barber.getId(), nextMonday)).doesNotContain(LocalTime.of(10, 0), LocalTime.of(10, 15));
    }

    @Test
    void rejeitaSobreposicaoPreservaPrecoECancelamentoLiberaHorario() {
        var first = agendamentoService.createPublic(form(LocalTime.of(9, 0)));
        service.setPreco(new BigDecimal("80.00"));
        servicos.save(service);
        assertThat(first.getPrecoServicoSnapshot()).isEqualByComparingTo("50.00");

        assertThatThrownBy(() -> agendamentoService.createPublic(form(LocalTime.of(9, 15))))
            .isInstanceOf(RegraNegocioException.class);

        agendamentoService.changeStatus(first.getId(), StatusAgendamento.CANCELADO, "cliente pediu");
        assertThatCode(() -> agendamentoService.createPublic(form(LocalTime.of(9, 0)))).doesNotThrowAnyException();
    }

    @Test
    void agendarEConcluirNaoGeramRecebimentoAutomatico() {
        var appt = agendamentoService.createPublic(form(LocalTime.of(9, 0)));
        agendamentoService.changeStatus(appt.getId(), StatusAgendamento.CONCLUIDO, "");
        assertThat(movements.findAll()).isEmpty();
        assertThat(agendamentos.findById(appt.getId()).orElseThrow().isPagamentoRecebido()).isFalse();
    }

    @Test
    void planoMensalVinculaAgendamentoEBloqueiaLimiteSemanal() {
        planoMensalService.salvarConfiguracao(barber.getId(), new BigDecimal("120.00"), 4, 1, true);
        var planForm = new PublicPlanoMensalForm();
        planForm.setBarbeiroId(barber.getId());
        planForm.setNomeCliente("Cliente Plano");
        planForm.setTelefoneCliente("(11) 99999-9999");
        var plan = planoMensalService.contratar(planForm);

        var appt = agendamentoService.createPublic(form(LocalTime.of(9, 0)));
        assertThat(appt.getPlanoMensal()).isNotNull();
        assertThat(appt.getPlanoMensal().getId()).isEqualTo(plan.getId());
        assertThat(appt.getCliente().getNome()).isEqualTo("Cliente Plano");

        assertThatThrownBy(() -> agendamentoService.createPublic(form(LocalTime.of(10, 0))))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessageContaining("semanal");
    }

    @Test
    void caixaImpedeDuplicidadeEstornaECalculaDinheiroFisico() {
        var cash = cashService.open(new BigDecimal("100.00"));
        var appt = agendamentoService.createPublic(form(LocalTime.of(9, 0)));
        agendamentoService.changeStatus(appt.getId(), StatusAgendamento.CONCLUIDO, "");

        var pix = cashService.receipt(appt.getId(), FormaPagamento.PIX);
        assertThat(cashService.dinheiroEsperado(cash.getId(), cash.getDinheiroInicial())).isEqualByComparingTo("100.00");
        assertThatThrownBy(() -> cashService.receipt(appt.getId(), FormaPagamento.PIX)).isInstanceOf(RegraNegocioException.class);

        cashService.reverse(pix.getId(), "erro");
        assertThat(agendamentos.findById(appt.getId()).orElseThrow().isPagamentoRecebido()).isFalse();
        cashService.receipt(appt.getId(), FormaPagamento.DINHEIRO);
        assertThat(cashService.dinheiroEsperado(cash.getId(), cash.getDinheiroInicial())).isEqualByComparingTo("150.00");

        cashService.close(new BigDecimal("150.00"));
        assertThatThrownBy(() -> cashService.manual(TipoMovimentoCaixa.SAIDA_MANUAL, BigDecimal.TEN, "despesa", "geral"))
            .isInstanceOf(RegraNegocioException.class);
    }

    private AgendamentoPublicoForm form(LocalTime time) {
        AgendamentoPublicoForm form = new AgendamentoPublicoForm();
        form.setServicoId(service.getId());
        form.setBarbeiroId(barber.getId());
        form.setData(nextMonday);
        form.setHora(time);
        form.setNomeCliente("Cliente");
        form.setTelefoneCliente("(11) 99999-9999");
        return form;
    }
}
