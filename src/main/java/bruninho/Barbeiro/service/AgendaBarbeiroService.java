package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ConfiguracaoBarbeariaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AgendaBarbeiroService {

    private static final int MAXIMO_ANTECEDENCIA_DIAS = 30;

    private final AgendamentoRepository agendamentos;
    private final ConfiguracaoBarbeariaRepository configuracoes;

    public AgendaBarbeiroService(AgendamentoRepository agendamentos, ConfiguracaoBarbeariaRepository configuracoes) {
        this.agendamentos = agendamentos;
        this.configuracoes = configuracoes;
    }

    public List<ItemAgendaBarbeiro> agendaDoDia(String loginBarbeiro, LocalDate data) {
        return agendamentos.findAllByBarbeiro_Usuario_LoginAndDataOrderByHoraInicioAsc(loginBarbeiro, data)
                .stream()
                .map(ItemAgendaBarbeiro::de)
                .toList();
    }

    public LocalDate dataMaximaAgenda(LocalDate hoje) {
        return hoje.plusDays(Math.max(antecedenciaConfigurada() - 1, 0));
    }

    public List<LocalDate> datasPermitidas(LocalDate hoje) {
        LocalDate dataMaxima = dataMaximaAgenda(hoje);
        return hoje.datesUntil(dataMaxima.plusDays(1)).toList();
    }

    public LocalDate ajustarDataSelecionada(LocalDate dataSelecionada, LocalDate hoje) {
        if (dataSelecionada == null || dataSelecionada.isBefore(hoje)) {
            return hoje;
        }

        LocalDate dataMaxima = dataMaximaAgenda(hoje);
        if (dataSelecionada.isAfter(dataMaxima)) {
            return dataMaxima;
        }

        return dataSelecionada;
    }

    public boolean dataFoiAjustada(LocalDate dataOriginal, LocalDate dataAjustada) {
        return dataOriginal != null && !dataOriginal.equals(dataAjustada);
    }

    private int antecedenciaConfigurada() {
        Integer dias = configuracoes.findById(1L)
                .map(config -> config.getDiasMaximoAntecedentia())
                .orElse(MAXIMO_ANTECEDENCIA_DIAS);

        if (dias == null || dias <= 0) {
            return 1;
        }

        return Math.min(dias, MAXIMO_ANTECEDENCIA_DIAS);
    }

    public record ItemAgendaBarbeiro(
            Long id,
            String nomeCliente,
            String telefoneCliente,
            LocalTime horaInicio,
            LocalTime horaFinalizacao,
            BigDecimal precoTotal
    ) {
        public static ItemAgendaBarbeiro de(Agendamento agendamento) {
            String nome = agendamento.getNomeCliente();
            String telefone = agendamento.getTelefoneCliente();

            if ((nome == null || nome.isBlank()) && agendamento.getCliente() != null) {
                nome = agendamento.getCliente().getNome();
            }
            if ((telefone == null || telefone.isBlank()) && agendamento.getCliente() != null) {
                telefone = agendamento.getCliente().getTelefone();
            }

            return new ItemAgendaBarbeiro(
                    agendamento.getId(),
                    nome,
                    telefone,
                    agendamento.getHoraInicio(),
                    agendamento.getHoraFinalizacao(),
                    agendamento.getPrecoTotal()
            );
        }
    }
}
