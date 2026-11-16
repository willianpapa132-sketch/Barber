package bruninho.Barbeiro.platform;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
import java.util.UUID;

/** A coluna gravavel e preenchida exclusivamente pelo contexto, nunca pelo JSON. */
@MappedSuperclass
@EntityListeners(EmpresaEntityListener.class)
@FilterDef(name = "empresa", autoEnabled = true, applyToLoadByKey = true,
        parameters = @ParamDef(name = "empresaId", type = UUID.class, resolver = EmpresaIdSupplier.class))
public abstract class EmpresaScopedEntity {

    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false, insertable = false, updatable = false)
    private Empresa empresa;

    @JsonIgnore
    public UUID getEmpresaId() { return empresaId; }
    @JsonIgnore
    public Empresa getEmpresa() { return empresa; }

    void bindEmpresa() {
        UUID current = TenantContext.requireEmpresaId();
        if (empresaId == null) empresaId = current;
        if (!current.equals(empresaId)) throw new IllegalStateException("Registro pertence a outra empresa");
    }
}
