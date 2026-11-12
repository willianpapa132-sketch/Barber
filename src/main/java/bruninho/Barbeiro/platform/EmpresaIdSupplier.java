package bruninho.Barbeiro.platform;

import java.util.UUID;
import java.util.function.Supplier;

public final class EmpresaIdSupplier implements Supplier<UUID> {
    @Override public UUID get() { return TenantContext.requireEmpresaId(); }
}
