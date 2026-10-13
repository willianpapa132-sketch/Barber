package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.perfil.AtualizarPerfilClienteRequest;
import bruninho.Barbeiro.Controller.cliente.perfil.ClientePerfilResponse;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.security.model.ROLE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional(readOnly = true)
    public ClientePerfilResponse buscarPerfil(String login) {
        return ClientePerfilResponse.de(clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado")));
    }

    @Transactional
    public ClientePerfilResponse atualizarPerfil(String login, AtualizarPerfilClienteRequest request) {
        Cliente cliente = clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        cliente.setNome(request.nome().trim());
        cliente.setTelefone(request.telefone().trim());
        cliente.setImgUrl(normalizarImagem(request.imgUrl()));
        cliente.setAtualizadoEm(LocalDateTime.now());

        return ClientePerfilResponse.de(clienteRepository.save(cliente));
    }




    private String normalizarImagem(String imgUrl) {
        if (imgUrl == null || imgUrl.isBlank()) {
            return null;
        }
        return imgUrl.trim();
    }
}
