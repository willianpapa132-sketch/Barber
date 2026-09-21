package bruninho.Barbeiro.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PublicPlanoMensalForm {
    @NotNull
    private Long barbeiroId;
    @NotBlank @Size(max = 120)
    private String nomeCliente;
    @NotBlank @Size(max = 30)
    private String telefoneCliente;

    public Long getBarbeiroId() { return barbeiroId; }
    public void setBarbeiroId(Long barbeiroId) { this.barbeiroId = barbeiroId; }
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    public String getTelefoneCliente() { return telefoneCliente; }
    public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }
}
