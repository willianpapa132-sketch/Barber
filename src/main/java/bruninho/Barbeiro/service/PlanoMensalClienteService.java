package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.request.CadastrarPlano;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.repository.BarbeiroRepository;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
public class PlanoMensalClienteService {


    private PlanoMensalRepository planoMensalRepository;
    private ClienteRepository clienteRepository;
    private BarbeiroRepository barbeiroRepository;
    private PlanoMensalClienteRepository planoMensalClienteRepository;


    public PlanoMensalClienteService(PlanoMensalRepository planoMensalRepository,  ClienteRepository clienteRepository,
            BarbeiroRepository barbeiroRepository, PlanoMensalClienteRepository planoMensalClienteRepository
    ) {
        this.planoMensalRepository = planoMensalRepository;
        this.clienteRepository = clienteRepository;
        this.barbeiroRepository = barbeiroRepository;
        this.planoMensalClienteRepository = planoMensalClienteRepository;

    }


    @Transactional
    public void cadastrarPlano(CadastrarPlano cadastrarPlano){

        PlanoMensalCliente planoMensalCliente = new PlanoMensalCliente();
        planoMensalCliente.setPlanoMensal(planoMensalRepository.findById(cadastrarPlano.getPlanoMensalid()).orElseThrow(()-> new RuntimeException("não localizado o plano selecionado")));
        planoMensalCliente.setCliente(clienteRepository.findById(cadastrarPlano.getClienteid()).orElseThrow(()-> new RuntimeException("não localizado o cliente")));
        planoMensalCliente.setBarbeiro(barbeiroRepository.findById(cadastrarPlano.getBarbeiroid()).orElseThrow(()-> new RuntimeException("barbeiro selecionado nãa localizado")));

        if(!planoMensalCliente.getCliente().getUsuario().isAtivo()){
            throw new RuntimeException("cliente esta desativado");
        }
        if(!planoMensalCliente.getBarbeiro().getUsuario().isAtivo()){
            throw new RuntimeException("barbeiro esta desativado");
        }
        if(!planoMensalCliente.getPlanoMensal().getAtivo()){
            throw new RuntimeException("planoMensal esta desativado");
        }

        planoMensalCliente.setValorMensal(planoMensalCliente.getValorMensal());
        if(planoMensalCliente.getPlanoMensal().getAgendamentosIndeterminado() == false){
            planoMensalCliente.setAgendamentosMes(planoMensalCliente.getPlanoMensal().getAgendamentoMes());
            planoMensalCliente.setAgendamentosSemana(planoMensalCliente.getPlanoMensal().getAgendamentoSemana());
        }else{
            planoMensalCliente.setAgendamentosMes(0);
            planoMensalCliente.setAgendamentosSemana(0);
            planoMensalCliente.setAgendamentosIndeterminado(true);
        }
        planoMensalCliente.setCriadoEm(LocalDateTime.now());
        planoMensalCliente.setAtivo(true);
        planoMensalClienteRepository.save(planoMensalCliente);
    }
}
