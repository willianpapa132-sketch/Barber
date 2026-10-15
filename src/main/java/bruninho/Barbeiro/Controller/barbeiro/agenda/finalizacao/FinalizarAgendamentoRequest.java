package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import bruninho.Barbeiro.domain.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record FinalizarAgendamentoRequest(@NotNull FormaPagamento formaPagamento) {}
