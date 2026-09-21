package bruninho.Barbeiro.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public class AgendamentoForm {
    private Long clienteId;
    @NotBlank @Size(max = 120) private String nomeCliente;
    @NotBlank @Size(max = 30) private String telefoneCliente;
    @NotNull private Long servicoId;
    @NotNull private Long barbeiroId;
    @NotNull private LocalDate data;
    @NotNull private LocalTime hora;

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getNomeCliente() { return nomeCliente; }
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }
    public String getTelefoneCliente() { return telefoneCliente; }
    public void setTelefoneCliente(String telefoneCliente) { this.telefoneCliente = telefoneCliente; }
    public Long getServicoId() { return servicoId; }
    public void setServicoId(Long servicoId) { this.servicoId = servicoId; }
    public Long getBarbeiroId() { return barbeiroId; }
    public void setBarbeiroId(Long barbeiroId) { this.barbeiroId = barbeiroId; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }
}
