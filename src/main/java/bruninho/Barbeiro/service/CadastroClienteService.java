package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.auth.CadastroClienteRequest;
import bruninho.Barbeiro.Controller.auth.CadastroClienteResponse;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.model.Usuario;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CadastroClienteService {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final PasswordEncoder passwordEncoder;

    public CadastroClienteService(
            UsuarioRepository usuarios,
            ClienteRepository clientes,
            PasswordEncoder passwordEncoder
    ) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CadastroClienteResponse cadastrar(CadastroClienteRequest request) {
        String login = request.email().trim().toLowerCase();

        if (!request.senha().equals(request.confirmarSenha())) {
            throw new BusinessException("As senhas nao conferem.");
        }

        if (usuarios.existsByLoginIgnoreCase(login)) {
            throw new BusinessException("Este e-mail ja esta cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setLogin(login);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setRole(ROLE.CLIENTE);
        usuario.setAtivo(true);
        usuario = usuarios.save(usuario);

        Cliente cliente = new Cliente();
        cliente.setNome(request.nome().trim());
        cliente.setTelefone(request.telefone().trim());
        cliente.setUsuario(usuario);
        cliente.setCriadoEm(LocalDateTime.now());
        cliente.setAtualizadoEm(LocalDateTime.now());
        cliente = clientes.save(cliente);

        return new CadastroClienteResponse(
                usuario.getId(),
                cliente.getId(),
                usuario.getLogin(),
                cliente.getNome()
        );
    }
}
