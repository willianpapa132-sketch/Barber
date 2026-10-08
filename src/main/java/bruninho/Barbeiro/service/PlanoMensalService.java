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
import java.util.Set;

@Service
public class PlanoMensalService {

    private final PlanoMensalRepository planoMensalRepository;
    private final ServicoRepository servicoRepository;
    private final PlanoMensalClienteRepository planoMensalClienteRepository;

    public PlanoMensalService(
            PlanoMensalRepository planoMensalRepository,
            ServicoRepository servicoRepository,
            PlanoMensalClienteRepository planoMensalClienteRepository
    ) {
        this.planoMensalRepository = planoMensalRepository;
        this.servicoRepository = servicoRepository;
        this.planoMensalClienteRepository = planoMensalClienteRepository;
    }

    @Transactional
    public void CadastrarPlanosMensais(CriarPlanoMensal criarPlanoMensal) {
        PlanosMensal planosMensal = new PlanosMensal();
        planosMensal.setNomePlano(criarPlanoMensal.getNomePlano());
        planosMensal.setValorMensal(criarPlanoMensal.getValorMensal());
        planosMensal.setAtivo(criarPlanoMensal.isAtivo());
        planosMensal.setServicosIncluidos(buscarServicos(criarPlanoMensal.getServicosId()));
        planosMensal.setCriadoEm(LocalDateTime.now());
        planosMensal.setDiasMaximoAntecedencia(criarPlanoMensal.getDiasMaximoAntecedencia());
        planoMensalRepository.save(planosMensal);
    }

    @Transactional
    public void AtualizarPlanoMensal(AtualizarPlanoMensal atualizarPlanoMensal) {
        PlanosMensal planosMensalLocalizado = planoMensalRepository.findById(atualizarPlanoMensal.getId())
                .orElseThrow(() -> new RuntimeException("plano mensal nao localizado"));

        planosMensalLocalizado.setNomePlano(atualizarPlanoMensal.getNomePlano());
        planosMensalLocalizado.setValorMensal(atualizarPlanoMensal.getValorMensal());
        planosMensalLocalizado.setDiasMaximoAntecedencia(atualizarPlanoMensal.getDiasMaximoAntecedencia());
        planosMensalLocalizado.getServicosIncluidos().clear();
        planosMensalLocalizado.getServicosIncluidos().addAll(buscarServicos(atualizarPlanoMensal.getServicosId()));
        planosMensalLocalizado.setAtualizadoEm(LocalDateTime.now());

        planoMensalRepository.save(planosMensalLocalizado);
        atualizarPlanosContratados(planosMensalLocalizado);
    }

    private void atualizarPlanosContratados(PlanosMensal planoMensal) {
        for (PlanoMensalCliente planoCliente : planoMensalClienteRepository.findAllByPlanoMensal_Id(planoMensal.getId())) {
            planoCliente.setValorMensal(planoMensal.getValorMensal());
            planoCliente.setDiasMaximoAntecedencia(planoMensal.getDiasMaximoAntecedencia());
            planoCliente.setAtualizadoEm(LocalDateTime.now());
            planoMensalClienteRepository.save(planoCliente);
        }
    }

    private Set<Servico> buscarServicos(Set<Long> servicosIds) {
        Set<Servico> servicosIncluidos = new HashSet<>(servicoRepository.findAllById(servicosIds));

        if (servicosIncluidos.size() != servicosIds.size()) {
            throw new RuntimeException("servicos nao localizados");
        }

        return servicosIncluidos;
    }
}
