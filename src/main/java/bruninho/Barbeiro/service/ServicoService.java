package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.cadastros.servicos.SalvarServicoRequest;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.repository.AgendamentoRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Service
public class ServicoService {

    private ServicoRepository servicoRepository;
    private AgendamentoRepository agendamentoRepository;
    private PlanoMensalRepository planoMensalRepository;
    private final bruninho.Barbeiro.repository.ServicosPlanosDescontoRepository descontos;

    public ServicoService(ServicoRepository servicoRepository,  AgendamentoRepository agendamentoRepository, PlanoMensalRepository planoMensalRepository, bruninho.Barbeiro.repository.ServicosPlanosDescontoRepository descontos) {
        this.descontos = descontos;
        this.servicoRepository = servicoRepository;
        this.agendamentoRepository = agendamentoRepository;
        this.planoMensalRepository = planoMensalRepository;
    }


    @Transactional
    public String cadastrarServico(SalvarServicoRequest criacaoServico){

        validar(criacaoServico.nome(), criacaoServico.valor(), criacaoServico.duracaoMinutos());

        Servico servico = new Servico();
        servico.setNome(criacaoServico.nome().trim());
        servico.setPreco(criacaoServico.valor());
        servico.setDuracaoMinutos(criacaoServico.duracaoMinutos());
        servico.setAtivo(Boolean.TRUE.equals(criacaoServico.ativo()));
        servico.setCriadoEm(LocalDateTime.now());
        servico.setAtualizadoEm(null);
        servicoRepository.save(servico);
        return "Criado com sucesso!";
    }

    @Transactional
    public String atualizarServico(Long id, SalvarServicoRequest servicoAtualizar){

        validar(servicoAtualizar.nome(), servicoAtualizar.valor(), servicoAtualizar.duracaoMinutos());
        if (servicoAtualizar.ativo() == null) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Informe o status do serviço.");

        Servico servicoLocalizado = localizarServicoPorID(id);
        servicoLocalizado.setNome(servicoAtualizar.nome().trim());
        servicoLocalizado.setPreco(servicoAtualizar.valor());
        servicoLocalizado.setAtivo(servicoAtualizar.ativo());
        servicoLocalizado.setAtualizadoEm(LocalDateTime.now());
        servicoLocalizado.setDuracaoMinutos(servicoAtualizar.duracaoMinutos());


        servicoRepository.save(servicoLocalizado);
        return "Atualizado com sucesso!";
    }

    public Servico localizarServicoPorID(Long id){
        return servicoRepository.findById(id).orElseThrow(()-> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Serviço não encontrado.")) ;
    }

    @Transactional
    public String DeletarServicoPorID(Long id){
        Servico servico = localizarServicoPorID(id);

        if (possuiVinculos(id)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                    "Não é possível excluir: o serviço possui vínculos com agendamentos ou planos. Você pode desativá-lo.");
        }
        servicoRepository.delete(servico);
        return "Deletado com sucesso!";
    }

    public List<Servico> listarServicos(){
        return servicoRepository.findAll();
    }


    public boolean possuiVinculos(Long id) {
        return agendamentoRepository.existsByServico_Id(id)
                || agendamentoRepository.existsByServicosPlano_ServicoId(id)
                || planoMensalRepository.existsByServicosIncluidos_Id(id)
                || descontos.existsByServicos_Id(id);
    }

    private void validar(String nome, BigDecimal valor, int minutos) {
        if (nome == null || nome.isBlank() || nome.trim().length() > 255 || valor == null
                || valor.signum() < 0 || valor.scale() > 2 || valor.precision() - valor.scale() > 10 || minutos <= 0) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Informe nome (até 255 caracteres), preço não negativo com até duas casas decimais e duração maior que zero.");
        }
    }
}
