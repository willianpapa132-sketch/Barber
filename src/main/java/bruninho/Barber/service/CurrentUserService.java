package bruninho.Barber.service;

import bruninho.Barber.domain.AppUser;
import bruninho.Barber.repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final AppUserRepository users;

    public CurrentUserService(AppUserRepository users) {
        this.users = users;
    }

    public AppUser requiredUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessException("Usuário não autenticado.");
        }
        return users.findByUsername(auth.getName()).orElseThrow(() -> new BusinessException("Usuário não encontrado."));
    }
}
