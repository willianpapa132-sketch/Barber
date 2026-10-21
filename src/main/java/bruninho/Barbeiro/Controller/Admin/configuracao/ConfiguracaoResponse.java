package bruninho.Barbeiro.Controller.Admin.configuracao;
import java.util.List;
public record ConfiguracaoResponse(String nome, String telefone, String endereco, Integer diasMaximoAntecedencia,
        List<HorarioFuncionamentoBarber> horarios, int jornadasAjustadas, int agendamentosCancelados) {}
