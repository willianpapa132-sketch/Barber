package bruninho.Barbeiro.Controller.Admin.requests;

import jakarta.validation.constraints.*;
import java.util.Set;

public record DescontoServicoPlanoRequest(
        @NotNull @Min(0) @Max(100) Integer porcentagemDesconto,
        @NotEmpty Set<@NotNull @Positive Long> servicosIds) {}
