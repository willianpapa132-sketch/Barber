package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.domain.ConfiguracaoPlanoMensal;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ClienteRepositorio;
import bruninho.Barbeiro.repository.ConfiguracaoPlanoMensalRepositorio;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepositorio;
import bruninho.Barbeiro.web.form.PublicPlanoMensalForm;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlanoMensalServico {
    private static final List<StatusAgendamento> STATUS_QUE_CONSOMEM_PLANO = List.of(StatusAgendamento.AGENDADO, StatusAgendamento.CONCLUIDO);

    private final ConfiguracaoPlanoMensalRepositorio configuracoes;
    private final PlanoMensalClienteRepositorio planos;
    private final ClienteRepositorio clientes;
    private final BarbeiroRepositorio barbeiros;
    private final AgendamentoRepositorio agendamentos;
    private final NormalizadorTelefone telefones;

    public PlanoMensalServico(ConfiguracaoPlanoMensalRepositorio configuracoes, PlanoMensalClienteRepositorio planos,
                              ClienteRepositorio clientes, BarbeiroRepositorio barbeiros,
                              AgendamentoRepositorio agendamentos, NormalizadorTelefone telefones) {
        this.configuracoes = configuracoes;
        this.planos = planos;
        this.clientes = clientes;
        this.barbeiros = barbeiros;
        this.agendamentos = agendamentos;
        this.telefones = telefones;
    }

    @Transactional
    public ConfiguracaoPlanoMensal salvarConfiguracao(Long barbeiroId, BigDecimal valorMensal, int cortesPorMes, int cortesPorSemana, boolean ativo) {
        Barbeiro barbeiro = barbeiros.findById(barbeiroId).orElseThrow(() -> new RegraNegocioException("Barbeiro nao encontrado."));
        if (valorMensal == null || valorMensal.signum() < 0) throw new RegraNegocioException("Valor mensal invalido.");
        if (cortesPorMes < 1) throw new RegraNegocioException("Informe ao menos 1 corte por mes.");
        if (cortesPorSemana < 1) throw new RegraNegocioException("Informe ao menos 1 corte por semana.");
        if (cortesPorSemana > cortesPorMes) throw new RegraNegocioException("O limite semanal nao pode ser maior que o mensal.");
        ConfiguracaoPlanoMensal configuracao = configuracoes.findByBarbeiroId(barbeiroId).orElseGet(ConfiguracaoPlanoMensal::new);
        configuracao.setBarbeiro(barbeiro);
        configuracao.setValorMensal(valorMensal);
        configuracao.setCortesPorMes(cortesPorMes);
        configuracao.setCortesPorSemana(cortesPorSemana);
        configuracao.setAtivo(ativo);
        configuracao.touch();
        return configuracoes.save(configuracao);
    }

    @Transactional
    public PlanoMensalCliente contratar(PublicPlanoMensalForm form) {
        telefones.validate(form.getTelefoneCliente());
        String telefoneNormalizado = telefones.normalize(form.getTelefoneCliente());
        Barbeiro barbeiro = barbeiros.findById(form.getBarbeiroId()).orElseThrow(() -> new RegraNegocioException("Barbeiro nao encontrado."));
        if (!barbeiro.isAtivo()) throw new RegraNegocioException("Barbeiro indisponivel para novos planos.");
        ConfiguracaoPlanoMensal configuracao = configuracoes.findByBarbeiroId(barbeiro.getId())
            .filter(ConfiguracaoPlanoMensal::isAtivo)
            .orElseThrow(() -> new RegraNegocioException("Este barbeiro ainda nao possui plano mensal ativo."));
        LocalDate hoje = LocalDate.now();
        if (!planos.buscarAtivoPorTelefoneEBarbeiro(telefoneNormalizado, barbeiro.getId(), hoje).isEmpty()) {
            throw new RegraNegocioException("Ja existe um plano mensal ativo para este telefone com este barbeiro.");
        }
        Cliente cliente = clientes.findFirstByTelefoneNormalizadoOrderByIdAsc(telefoneNormalizado).orElseGet(Cliente::new);
        cliente.setNome(form.getNomeCliente().trim());
        cliente.setTelefone(form.getTelefoneCliente().trim());
        cliente.setTelefoneNormalizado(telefoneNormalizado);
        cliente.touch();
        clientes.save(cliente);

        PlanoMensalCliente plano = new PlanoMensalCliente();
        plano.setCliente(cliente);
        plano.setBarbeiro(barbeiro);
        plano.setValorMensalSnapshot(configuracao.getValorMensal());
        plano.setCortesPorMesSnapshot(configuracao.getCortesPorMes());
        plano.setCortesPorSemanaSnapshot(configuracao.getCortesPorSemana());
        plano.setDataInicio(hoje);
        return planos.save(plano);
    }

    @Transactional
    public void cancelar(Long planoId) {
        PlanoMensalCliente plano = planos.findByIdAndAtivoTrue(planoId).orElseThrow(() -> new RegraNegocioException("Plano ativo nao encontrado."));
        plano.setAtivo(false);
        plano.setDataFim(LocalDate.now());
        plano.touch();
    }

    public PlanoMensalCliente planoAtivoPara(String telefone, Long barbeiroId, LocalDate data) {
        String telefoneNormalizado = telefones.normalize(telefone);
        return planos.buscarAtivoPorTelefoneEBarbeiro(telefoneNormalizado, barbeiroId, data).stream().findFirst().orElse(null);
    }

    public void validarUsoDisponivel(PlanoMensalCliente plano, LocalDateTime inicio) {
        LocalDate data = inicio.toLocalDate();
        LocalDate primeiroDiaMes = data.withDayOfMonth(1);
        LocalDate primeiroDiaProximoMes = primeiroDiaMes.plusMonths(1);
        LocalDate primeiroDiaSemana = data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate primeiroDiaProximaSemana = primeiroDiaSemana.plusWeeks(1);
        long usadosNoMes = agendamentos.countByPlanoMensalIdAndStatusInAndInicioEmGreaterThanEqualAndInicioEmLessThan(
            plano.getId(), STATUS_QUE_CONSOMEM_PLANO, primeiroDiaMes.atStartOfDay(), primeiroDiaProximoMes.atStartOfDay());
        long usadosNaSemana = agendamentos.countByPlanoMensalIdAndStatusInAndInicioEmGreaterThanEqualAndInicioEmLessThan(
            plano.getId(), STATUS_QUE_CONSOMEM_PLANO, primeiroDiaSemana.atStartOfDay(), primeiroDiaProximaSemana.atStartOfDay());
        if (usadosNoMes >= plano.getCortesPorMesSnapshot()) {
            throw new RegraNegocioException("Limite mensal do plano atingido.");
        }
        if (usadosNaSemana >= plano.getCortesPorSemanaSnapshot()) {
            throw new RegraNegocioException("Limite semanal do plano atingido.");
        }
    }
}
