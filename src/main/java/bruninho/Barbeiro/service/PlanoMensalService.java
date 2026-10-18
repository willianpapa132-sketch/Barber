package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.requests.AtualizarPlanoMensal;
import bruninho.Barbeiro.Controller.Admin.requests.CriarPlanoMensal;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.ServicosPlanosDesconto;
import bruninho.Barbeiro.Controller.Admin.requests.DescontoServicoPlanoRequest;
import bruninho.Barbeiro.exception.BusinessException;
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
        planosMensal.setDescricao(criarPlanoMensal.getDescricao());
        configurarDescontos(planosMensal, criarPlanoMensal.getDescontosServicos());
        planoMensalRepository.save(planosMensal);
    }

    @Transactional
    public void AtualizarPlanoMensal(AtualizarPlanoMensal atualizarPlanoMensal) {
        PlanosMensal planosMensalLocalizado = planoMensalRepository.findById(atualizarPlanoMensal.getId())
                .orElseThrow(() -> new RuntimeException("plano mensal nao localizado"));

        planosMensalLocalizado.setNomePlano(atualizarPlanoMensal.getNomePlano());
        planosMensalLocalizado.setDescricao(atualizarPlanoMensal.getDescricao());
        planosMensalLocalizado.setValorMensal(atualizarPlanoMensal.getValorMensal());
        planosMensalLocalizado.setDiasMaximoAntecedencia(atualizarPlanoMensal.getDiasMaximoAntecedencia());
        planosMensalLocalizado.getServicosIncluidos().clear();
        planosMensalLocalizado.getServicosIncluidos().addAll(buscarServicos(atualizarPlanoMensal.getServicosId()));
        planosMensalLocalizado.setAtualizadoEm(LocalDateTime.now());

        configurarDescontos(planosMensalLocalizado, atualizarPlanoMensal.getDescontosServicos());
        planoMensalRepository.save(planosMensalLocalizado);
        atualizarPlanosContratados(planosMensalLocalizado);
    }

    private void configurarDescontos(PlanosMensal plano, java.util.List<DescontoServicoPlanoRequest> descontos) {
        // Campo omitido preserva as regras; lista vazia remove todos os descontos.
        if (descontos == null) return;
        var usados = new HashSet<Long>();
        var novasRegras = new HashSet<ServicosPlanosDesconto>();
        for (var desconto : descontos) {
            if (desconto == null || desconto.porcentagemDesconto() == null
                    || desconto.porcentagemDesconto() < 0 || desconto.porcentagemDesconto() > 100
                    || desconto.servicosIds() == null || desconto.servicosIds().isEmpty()
                    || desconto.servicosIds().stream().anyMatch(id -> id == null || id <= 0)) {
                throw new BusinessException("Informe servicos e desconto entre 0 e 100");
            }
            for (var id : desconto.servicosIds()) {
                if (!usados.add(id)) throw new BusinessException("Servico com mais de uma regra de desconto no plano");
            }
            var regra = new ServicosPlanosDesconto();
            regra.setPlanoMensal(plano);
            regra.setPorcentagemDesconto(desconto.porcentagemDesconto());
            regra.setServicos(buscarServicos(desconto.servicosIds()));
            novasRegras.add(regra);
        }
        plano.getDescontosServicos().clear();
        plano.getDescontosServicos().addAll(novasRegras);
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
