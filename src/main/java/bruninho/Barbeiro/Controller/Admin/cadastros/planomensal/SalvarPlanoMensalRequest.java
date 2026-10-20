package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record SalvarPlanoMensalRequest(
        @NotBlank @Size(max = 255) String nomePlano,
        @NotBlank @Size(max = 255) String descricao,
        @NotNull @Positive @Digits(integer = 10, fraction = 2) BigDecimal valorMensal,
        @NotNull Boolean ativo,
        @NotNull @Positive Integer diasMaximoAntecedencia,
        @NotEmpty Set<@NotNull @Positive Long> servicosId,
        @Valid List<@NotNull DescontoServicoPlanoRequest> descontosServicos
) {}
