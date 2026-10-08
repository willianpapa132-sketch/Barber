package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.request.CadastrarPlano;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class PlanoMensalClienteService {

    private final PlanoMensalRepository planoMensalRepository;
    private final ClienteRepository clienteRepository;
    private final PlanoMensalClienteRepository planoMensalClienteRepository;

    public PlanoMensalClienteService(
            PlanoMensalRepository planoMensalRepository,
            ClienteRepository clienteRepository,
            PlanoMensalClienteRepository planoMensalClienteRepository
    ) {
        this.planoMensalRepository = planoMensalRepository;
        this.clienteRepository = clienteRepository;
        this.planoMensalClienteRepository = planoMensalClienteRepository;
    }

    @Transactional
    public void cadastrarPlano(CadastrarPlano cadastrarPlano) {
        PlanoMensalCliente planoMensalCliente = new PlanoMensalCliente();
        planoMensalCliente.setPlanoMensal(planoMensalRepository.findById(cadastrarPlano.getPlanoMensalid())
                .orElseThrow(() -> new RuntimeException("nao localizado o plano selecionado")));
        planoMensalCliente.setCliente(clienteRepository.findById(cadastrarPlano.getClienteid())
                .orElseThrow(() -> new RuntimeException("nao localizado o cliente")));
        planoMensalCliente.setUsuario(planoMensalCliente.getCliente().getUsuario());

        if (!Boolean.TRUE.equals(planoMensalCliente.getCliente().getUsuario().getAtivo())) {
            throw new RuntimeException("cliente esta desativado");
        }
        if (!Boolean.TRUE.equals(planoMensalCliente.getPlanoMensal().getAtivo())) {
            throw new RuntimeException("planoMensal esta desativado");
        }

        planoMensalCliente.setValorMensal(planoMensalCliente.getPlanoMensal().getValorMensal());
        planoMensalCliente.setDiasMaximoAntecedencia(planoMensalCliente.getPlanoMensal().getDiasMaximoAntecedencia());
        planoMensalCliente.setCriadoEm(LocalDateTime.now());
        planoMensalCliente.setAtivo(true);
        planoMensalClienteRepository.save(planoMensalCliente);
    }
}
