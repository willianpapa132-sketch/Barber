package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.requests.CriarPlanoMensal;
import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class PlanoMensalService {

    private PlanoMensalRepository planoMensalRepository;
    private ServicoRepository servicoRepository;

    public PlanoMensalService(PlanoMensalRepository planoMensalRepository, ServicoRepository servicoRepository) {
        this.planoMensalRepository = planoMensalRepository;
        this.servicoRepository = servicoRepository;
    }



    public void CadastrarPlanosMensais(CriarPlanoMensal criarPlanoMensal){
        PlanosMensal planosMensal = new PlanosMensal();
        if(criarPlanoMensal.getAgendamentoMes() == 0 || criarPlanoMensal.getAgendamentosSemana() == 0){
            planosMensal.setPrazoIndeterminado(true);
            planosMensal.setAgendamentoMes(0);
            planosMensal.setAgendamentoSemana(0);
        }else{
            planosMensal.setPrazoIndeterminado(false);
            planosMensal.setAgendamentoMes(criarPlanoMensal.getAgendamentoMes());
            planosMensal.setAgendamentoSemana(criarPlanoMensal.getAgendamentosSemana());
        }
        planosMensal.setNomePlano(criarPlanoMensal.getNomePlano());
        planosMensal.setValorMensal(criarPlanoMensal.getValorMensal());

        Set<Servico> servicosIncluidos = new HashSet<>(
                servicoRepository.findAllById(criarPlanoMensal.getServicosId())
        );

        if(servicosIncluidos.size() != criarPlanoMensal.getServicosId().size()){
            throw new RuntimeException("serviços não localizados");
        }

        planosMensal.setServicosIncluidos(servicosIncluidos);
        planosMensal.setCriadoEm(LocalDateTime.now());



    }
    public void AtualizarPlanoMensal(){

    }
}
