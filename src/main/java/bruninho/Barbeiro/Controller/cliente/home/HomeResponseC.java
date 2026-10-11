package bruninho.Barbeiro.Controller.cliente.home;

public record HomeResponseC(
        ClienteHomeResponse cliente,
        ProximoAgendamentoHomeResponse proximoAtendimento
) {
}
