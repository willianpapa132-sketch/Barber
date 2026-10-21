package bruninho.Barbeiro.Controller.Admin.configuracao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record SalvarConfiguracaoRequest(@NotBlank @Size(max=150) String nome,
        @Size(max=30) String telefone, @Size(max=250) String endereco,
        @NotNull @Min(0) @Max(30) Integer diasMaximoAntecedencia,
        @NotNull @Size(min=7,max=7) List<@NotNull @Valid HorarioFuncionamentoBarber> horarios) {}
