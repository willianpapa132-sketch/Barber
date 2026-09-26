package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.security.model.Perfil;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ClienteService {

    private ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }


    @Transactional
    public void criarCliente(Cliente cliente){
        if(cliente.getUsuario().getPerfil() != Perfil.CLIENTE){
            throw new RuntimeException("Usuario deve ter perfil de cliente");
        }
        cliente.setCriadoEm(LocalDateTime.now());
        clienteRepository.save(cliente);
    }

    @Transactional
    public void atualizarCliente(Cliente cliente){
        Cliente clienteLocalizado = procurarCliente(cliente.getId());
        clienteLocalizado.setNome(cliente.getNome());
        clienteLocalizado.setAtualizadoEm(LocalDateTime.now());
        if(cliente.getUsuario() != clienteLocalizado.getUsuario()){
            throw new RuntimeException("Usuario inconsistente com cliente");
        }
    }
    public Cliente procurarCliente(Long id){
        return clienteRepository.findById(id).orElseThrow(()-> new RuntimeException("Cliente não localizado"));
    }

}
