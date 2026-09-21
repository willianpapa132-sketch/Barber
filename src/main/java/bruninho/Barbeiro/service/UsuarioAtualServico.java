package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Usuario;
import bruninho.Barbeiro.repository.UsuarioRepositorio;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioAtualServico {
    private final UsuarioRepositorio usuarios;

    public UsuarioAtualServico(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario requiredUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RegraNegocioException("Usuário não autenticado.");
        }
        return usuarios.findByLogin(auth.getName()).orElseThrow(() -> new RegraNegocioException("Usuário não encontrado."));
    }
}
