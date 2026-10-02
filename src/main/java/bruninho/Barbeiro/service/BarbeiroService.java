package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.model.Usuario;
import org.springframework.stereotype.Service;


@Service
public class BarbeiroService {


    private BarbeiroRepository barbeiroRepository;
    public BarbeiroService(BarbeiroRepository barbeiroRepository) {
        this.barbeiroRepository = barbeiroRepository;
    }

    public void criarBarbeiro(Barbeiro barbeiro, Usuario usuario){
        if(usuario.getRole() == ROLE.BARBEIRO){
            barbeiroRepository.save(barbeiro);

        }else{
            throw new RuntimeException("deve ser informado um perfil de barbeiro");
        }
    }
    public void atualizarBarbeiro(Barbeiro barbeiro){
        barbeiroRepository.save(barbeiro);
    }


}
