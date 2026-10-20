package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.cadastros.planomensal.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PlanoMensalService {
    private final PlanoMensalRepository planoMensalRepository;
    private final ServicoRepository servicoRepository;
    private final PlanoMensalClienteRepository planoMensalClienteRepository;

    public PlanoMensalService(PlanoMensalRepository planos, ServicoRepository servicos, PlanoMensalClienteRepository contratos) {
        this.planoMensalRepository = planos;
        this.servicoRepository = servicos;
        this.planoMensalClienteRepository = contratos;
    }

    @Transactional(readOnly = true)
    public ListagemPlanosMensaisResponse listar(String busca, Boolean ativo, int pagina) {
        if (pagina < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        var page = planoMensalRepository.listarParaAdmin(busca.trim(), ativo,
                PageRequest.of(pagina, 10, Sort.by("nomePlano").and(Sort.by("id"))));
        return new ListagemPlanosMensaisResponse(page.getContent().stream().map(this::resposta).toList(),
                page.getTotalElements(), page.getTotalPages(), pagina);
    }

    @Transactional(readOnly = true)
    public List<ServicoPlanoAdminResponse> listarServicos() {
        return servicoRepository.findAll(Sort.by("nome").and(Sort.by("id"))).stream()
                .map(ServicoPlanoAdminResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public PlanoMensalAdminResponse consultar(Long id) { return resposta(localizar(id)); }

    @Transactional
    public PlanoMensalAdminResponse cadastrar(SalvarPlanoMensalRequest request) {
        var plano = new PlanosMensal();
        preencher(plano, request);
        plano.setCriadoEm(LocalDateTime.now());
        return resposta(planoMensalRepository.save(plano));
    }

    @Transactional
    public PlanoMensalAdminResponse atualizar(Long id, SalvarPlanoMensalRequest request) {
        var plano = localizar(id);
        preencher(plano, request);
        plano.setAtualizadoEm(LocalDateTime.now());
        atualizarPlanosContratados(plano);
        return resposta(plano);
    }

    @Transactional
    public void excluir(Long id) {
        var plano = localizar(id);
        if (planoMensalClienteRepository.existsByPlanoMensal_id(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não é possível excluir um plano com contratações. Você pode desativá-lo.");
        }
        planoMensalRepository.delete(plano);
        planoMensalRepository.flush();
    }

    private PlanosMensal localizar(Long id) {
        return planoMensalRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano mensal não encontrado."));
    }

    private void preencher(PlanosMensal plano, SalvarPlanoMensalRequest r) {
        if (r == null || r.nomePlano() == null || r.nomePlano().isBlank() || r.nomePlano().trim().length() > 255
                || r.descricao() == null || r.descricao().isBlank() || r.descricao().trim().length() > 255
                || r.valorMensal() == null || r.valorMensal().signum() <= 0 || r.valorMensal().scale() > 2
                || r.valorMensal().precision() - r.valorMensal().scale() > 10 || r.ativo() == null
                || r.diasMaximoAntecedencia() == null || r.diasMaximoAntecedencia() <= 0
                || r.servicosId() == null || r.servicosId().isEmpty()) {
            throw new BusinessException("Informe nome, descrição, valor positivo, antecedência e ao menos um serviço incluído.");
        }
        var incluidos = buscarServicos(r.servicosId());
        plano.setNomePlano(r.nomePlano().trim());
        plano.setDescricao(r.descricao().trim());
        plano.setValorMensal(r.valorMensal());
        plano.setAtivo(r.ativo());
        plano.setDiasMaximoAntecedencia(r.diasMaximoAntecedencia());
        plano.getServicosIncluidos().clear();
        plano.getServicosIncluidos().addAll(incluidos);
        configurarDescontos(plano, r.descontosServicos());
    }

    private void configurarDescontos(PlanosMensal plano, List<DescontoServicoPlanoRequest> descontos) {
        // Campo omitido preserva as regras; lista vazia remove todos os descontos.
        var incluidos = plano.getServicosIncluidos().stream().map(Servico::getId).collect(java.util.stream.Collectors.toSet());
        if (descontos == null) {
            if (plano.getDescontosServicos().stream().flatMap(d -> d.getServicos().stream()).anyMatch(s -> incluidos.contains(s.getId()))) {
                throw new BusinessException("Serviço incluído no plano não pode ter regra de desconto adicional.");
            }
            return;
        }
        var usados = new HashSet<Long>();
        var novasRegras = new HashSet<ServicosPlanosDesconto>();
        for (var desconto : descontos) {
            if (desconto == null || desconto.porcentagemDesconto() == null
                    || desconto.porcentagemDesconto() < 0 || desconto.porcentagemDesconto() > 100
                    || desconto.servicosIds() == null || desconto.servicosIds().isEmpty()) {
                throw new BusinessException("Informe serviços e desconto entre 0 e 100.");
            }
            for (var id : desconto.servicosIds()) {
                if (incluidos.contains(id)) throw new BusinessException("Serviço incluído no plano não pode ter regra de desconto adicional.");
                if (!usados.add(id)) throw new BusinessException("Serviço com mais de uma regra de desconto no plano.");
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

    private void atualizarPlanosContratados(PlanosMensal plano) {
        for (var contrato : planoMensalClienteRepository.findAllByPlanoMensal_Id(plano.getId())) {
            contrato.setValorMensal(plano.getValorMensal());
            contrato.setDiasMaximoAntecedencia(plano.getDiasMaximoAntecedencia());
            contrato.setAtualizadoEm(LocalDateTime.now());
        }
    }

    private Set<Servico> buscarServicos(Set<Long> ids) {
        if (ids == null || ids.stream().anyMatch(id -> id == null || id <= 0)) throw new BusinessException("Informe serviços válidos.");
        var servicos = new HashSet<>(servicoRepository.findAllById(ids));
        if (servicos.size() != ids.size()) throw new BusinessException("Um ou mais serviços não foram encontrados.");
        return servicos;
    }

    private List<ServicoPlanoAdminResponse> servicosOrdenados(Set<Servico> servicos) {
        return servicos.stream().sorted(Comparator.comparing(Servico::getNome).thenComparing(Servico::getId))
                .map(ServicoPlanoAdminResponse::de).toList();
    }

    private PlanoMensalAdminResponse resposta(PlanosMensal plano) {
        var descontos = plano.getDescontosServicos().stream()
                .sorted(Comparator.comparing(ServicosPlanosDesconto::getPorcentagemDesconto))
                .map(d -> new DescontoPlanoAdminResponse(d.getPorcentagemDesconto(), servicosOrdenados(d.getServicos()))).toList();
        return new PlanoMensalAdminResponse(plano.getId(), plano.getNomePlano(), plano.getDescricao(), plano.getValorMensal(),
                Boolean.TRUE.equals(plano.getAtivo()), plano.getDiasMaximoAntecedencia(), servicosOrdenados(plano.getServicosIncluidos()),
                descontos, !planoMensalClienteRepository.existsByPlanoMensal_id(plano.getId()));
    }
}
