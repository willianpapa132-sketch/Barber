package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.model.Usuario;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class ValidacaoAgendamentoService {

    private AgendamentoRepository agendamentoRepository;
    private UsuarioRepository usuarioRepository;
    private BarbeiroRepository barbeiroRepository;
    private ClienteRepository clienteRepository;


    public  ValidacaoAgendamentoService(
            AgendamentoRepository agendamentoRepository,
            BarbeiroRepository barbeiroRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            ClienteService clienteService
    ) {
        this.agendamentoRepository = agendamentoRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;

    }

    public void validaAgendamento(Agendamento agendamento) {

        Usuario usuario = usuarioRepository.findByLogin(agendamento.getCliente().getUsuario().getLogin())
                .orElseThrow(()-> new RuntimeException("não localizado nenhum usuario com o login informado"));

        if(usuario.getRole() != ROLE.CLIENTE){
            throw new RuntimeException("so pode ser criado um agendamento com o perfil de Usuario");
        }


    }

}
