package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.requests.AtualizarPlanoMensal;
import bruninho.Barbeiro.Controller.Admin.requests.CriarPlanoMensal;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import bruninho.Barbeiro.repository.ServicoRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PlanoMensalService {

    private PlanoMensalRepository planoMensalRepository;
    private ServicoRepository servicoRepository;
    private PlanoMensalClienteRepository planoMensalClienteRepository;

    public PlanoMensalService(PlanoMensalRepository planoMensalRepository, ServicoRepository servicoRepository,PlanoMensalClienteRepository planoMensalClienteRepository) {
        this.planoMensalRepository = planoMensalRepository;
        this.servicoRepository = servicoRepository;
        this.planoMensalClienteRepository =  planoMensalClienteRepository;
    }



    @Transactional
    public void CadastrarPlanosMensais(CriarPlanoMensal criarPlanoMensal){
        PlanosMensal planosMensal = new PlanosMensal();
        if(criarPlanoMensal.getAgendamentoMes() == 0 || criarPlanoMensal.getAgendamentosSemana() == 0){
            planosMensal.setAgendamentosIndeterminado(true);
            planosMensal.setAgendamentoMes(0);
            planosMensal.setAgendamentoSemana(0);
        }else{
            planosMensal.setAgendamentosIndeterminado(false);
            planosMensal.setAgendamentoMes(criarPlanoMensal.getAgendamentoMes());
            planosMensal.setAgendamentoSemana(criarPlanoMensal.getAgendamentosSemana());
        }
        planosMensal.setNomePlano(criarPlanoMensal.getNomePlano());
        planosMensal.setValorMensal(criarPlanoMensal.getValorMensal());

        Set<Servico> servicosIncluidos = new HashSet<>(
                servicoRepository.findAllById(criarPlanoMensal.getServicosId())
        );

        if(servicosIncluidos.size() != criarPlanoMensal.getServicosId().size()){
            throw new RuntimeException("serviços não localizados entre em contato com o willian");
        }

        planosMensal.getServicosIncluidos().addAll(servicosIncluidos);
        planosMensal.setCriadoEm(LocalDateTime.now());
        planosMensal.setDiasMaximoAntecedencia(criarPlanoMensal.getDiasMaximoAntecedencia());
        planoMensalRepository.save(planosMensal);


    }


    @Transactional
    public void AtualizarPlanoMensal(AtualizarPlanoMensal atualizarPlanoMensal){
        PlanosMensal planosMensalLocalizado = planoMensalRepository.findById(atualizarPlanoMensal.getId()).orElseThrow(()-> new RuntimeException("plano mensal não localizado"));
        planosMensalLocalizado.setNomePlano(atualizarPlanoMensal.getNomePlano());
        planosMensalLocalizado.setValorMensal(atualizarPlanoMensal.getValorMensal());
        planosMensalLocalizado.setDiasMaximoAntecedencia(atualizarPlanoMensal.getDiasMaximoAntecedencia());

        Set<Servico> servicosIncluidos = new HashSet<>(
                servicoRepository.findAllById(atualizarPlanoMensal.getServicosId())
        );

        if(servicosIncluidos.size() != atualizarPlanoMensal.getServicosId().size()){
            throw new RuntimeException("itens não localizados, entre em contato com o willian");
        }
        planosMensalLocalizado.getServicosIncluidos().clear();
        planosMensalLocalizado.getServicosIncluidos().addAll(servicosIncluidos);

        if(atualizarPlanoMensal.getAgendamentosMensal() == 0 || atualizarPlanoMensal.getAgendamentosSemana() == 0 ){
            planosMensalLocalizado.setAgendamentosIndeterminado(true);
            planosMensalLocalizado.setAgendamentoSemana(0);
            planosMensalLocalizado.setAgendamentoMes(0);
        }
        else{
            planosMensalLocalizado.setAgendamentosIndeterminado(false);
            planosMensalLocalizado.setAgendamentoSemana(atualizarPlanoMensal.getAgendamentosSemana());
            planosMensalLocalizado.setAgendamentoMes(atualizarPlanoMensal.getAgendamentosMensal());
        }
        planosMensalLocalizado.setAtualizadoEm(LocalDateTime.now());
        if(planoMensalClienteRepository.existsByPlanoMensal_id(atualizarPlanoMensal.getId())){
            List <PlanoMensalCliente> planosMensalClientes = planoMensalClienteRepository.findAllByPlanoMensal_Id(atualizarPlanoMensal.getId());
            for(PlanoMensalCliente planosMensaisClientes : planosMensalClientes){
                planosMensaisClientes.setValorMensal(atualizarPlanoMensal.getValorMensal());
                if(planosMensalLocalizado.getAgendamentosIndeterminado()){
                    planosMensaisClientes.setAgendamentosIndeterminado(true);
                    planosMensaisClientes.setAgendamentosSemana(0);
                    planosMensaisClientes.setAgendamentosMes(0);

                }
                else{
                    planosMensaisClientes.setAgendamentosIndeterminado(false);
                    planosMensaisClientes.setAgendamentosMes(atualizarPlanoMensal.getAgendamentosMensal());
                    planosMensaisClientes.setAgendamentosSemana(atualizarPlanoMensal.getAgendamentosSemana());
                }
                planosMensaisClientes.setAtualizadoEm(LocalDateTime.now());
                planosMensalLocalizado.setDiasMaximoAntecedencia(atualizarPlanoMensal.getDiasMaximoAntecedencia());
            }
        }

    }
}
