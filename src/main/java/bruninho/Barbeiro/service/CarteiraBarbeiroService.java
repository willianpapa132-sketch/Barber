package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.barbeiro.carteira.CarteiraBarbeiroResponse;
import bruninho.Barbeiro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;

@Service
public class CarteiraBarbeiroService {
    private final BarbeiroRepository barbeiros;
    private final AgendamentoRepository agendamentos;
    private final ComissaoPlanoRepository pontos;
    private final ComissaoBarbeiroRepository comissoes;

    public CarteiraBarbeiroService(BarbeiroRepository barbeiros, AgendamentoRepository agendamentos,
                                  ComissaoPlanoRepository pontos, ComissaoBarbeiroRepository comissoes) {
        this.barbeiros = barbeiros; this.agendamentos = agendamentos;
        this.pontos = pontos; this.comissoes = comissoes;
    }

    @Transactional(readOnly = true)
    public CarteiraBarbeiroResponse consultar(String login, LocalDate inicio, LocalDate fim) {
        validarPeriodo(inicio, fim);
        var barbeiro = barbeiros.findByUsuario_Login(login)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var id = barbeiro.getId();
        return new CarteiraBarbeiroResponse(inicio, fim,
                agendamentos.somarMinutosConcluidos(id, inicio, fim),
                pontos.somarPontosConcluidos(id, inicio, fim),
                comissoes.somarComissoesConcluidas(id, inicio, fim).setScale(2));
    }
    public static void validarPeriodo(LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um periodo com inicio e fim validos");
        }
        // Datas inclusivas: 01/01 a 31/03 corresponde a tres meses.
        if (fim.isAfter(inicio.plusMonths(3).minusDays(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O periodo deve ter no maximo 3 meses");
        }
    }
}
