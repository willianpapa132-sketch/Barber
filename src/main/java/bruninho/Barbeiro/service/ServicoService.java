package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.DTOS.CriacaoServico;
import bruninho.Barbeiro.Controller.Admin.DTOS.ServicoAtualizar;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ServicoService {

    private ServicoRepository servicoRepository;

    private AgendamentoRepository agendamentoRepository;

    public ServicoService(ServicoRepository servicoRepository,  AgendamentoRepository agendamentoRepository) {
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
    }


    @Transactional
    public String cadastrarServico(CriacaoServico criacaoServico){

        if(criacaoServico.getValor().compareTo(BigDecimal.ZERO) < 0 ){
            throw new RuntimeException("não pode ter valor menor que zero");
        }

        Servico servico = new Servico();
        servico.setNome(criacaoServico.getNome());
        servico.setPreco(criacaoServico.getValor());
        servico.setDuracaoMinutos(criacaoServico.getDuracaoMinutos());
        servico.setCriadoEm(LocalDateTime.now());
        servico.setAtualizadoEm(null);
        servicoRepository.save(servico);
        return "Criado com sucesso!";
    }

    @Transactional
    public String atualizarServico(ServicoAtualizar servicoAtualizar){

        if(servicoAtualizar.getValor().compareTo(BigDecimal.ZERO) < 0 ){
            throw new RuntimeException("valor tem que ser maior que zero");
        }

        Servico servicoLocalizado = localizarServicoPorID(servicoAtualizar.getId());
        servicoLocalizado.setNome(servicoAtualizar.getNome());
        servicoLocalizado.setPreco(servicoAtualizar.getValor());
        servicoLocalizado.setAtivo(servicoAtualizar.getAtivo());
        servicoLocalizado.setAtualizadoEm(LocalDateTime.now());
        servicoLocalizado.setDuracaoMinutos(servicoAtualizar.getTempoMinutos());

        servicoRepository.save(servicoLocalizado);
        return "Atualizado com sucesso!";
    }

    private Servico localizarServicoPorID(Long id){
        return servicoRepository.findById(id).orElseThrow(()-> new RuntimeException("não foi localizado o servico")) ;
    }

    public String DeletarServicoPorID(Long id){
        Servico servico = localizarServicoPorID(id);
        if(agendamentoRepository.existsByServicoID(servico.getId())){
            throw new RuntimeException("não pode ser feito a exclusão desse servico pois ele esta em outros agendamentos");
        }

        servicoRepository.delete(servico);
    }


}
