package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.cliente.planos.CadastrarPlano;
import bruninho.Barbeiro.Controller.cliente.planos.PlanoClientePageResponse;
import bruninho.Barbeiro.Controller.cliente.planos.PlanoContratadoResponse;
import bruninho.Barbeiro.Controller.cliente.planos.PlanoMensalResponse;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.PlanosMensal;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.repository.ClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalClienteRepository;
import bruninho.Barbeiro.repository.PlanoMensalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PlanoMensalClienteService {

    private final PlanoMensalRepository planoMensalRepository;
    private final ClienteRepository clienteRepository;
    private final PlanoMensalClienteRepository planoMensalClienteRepository;

    public PlanoMensalClienteService(
            PlanoMensalRepository planoMensalRepository,
            ClienteRepository clienteRepository,
            PlanoMensalClienteRepository planoMensalClienteRepository
    ) {
        this.planoMensalRepository = planoMensalRepository;
        this.clienteRepository = clienteRepository;
        this.planoMensalClienteRepository = planoMensalClienteRepository;
    }

    @Transactional(readOnly = true)
    public PlanoClientePageResponse buscarTelaPlanos(String login) {
        var cliente = clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        var planoAtual = planoMensalClienteRepository.findByCliente_IdAndAtivoTrue(cliente.getId())
                .map(PlanoContratadoResponse::de)
                .orElse(null);

        var planosDisponiveis = planosDisponiveis(planoAtual);

        return new PlanoClientePageResponse(
                planoAtual != null,
                planoAtual,
                planosDisponiveis
        );
    }

    @Transactional
    public PlanoContratadoResponse contratarPlano(String login, Long planoId) {
        var cliente = clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        if (!Boolean.TRUE.equals(cliente.getUsuario().getAtivo())) {
            throw new BusinessException("Cliente esta desativado.");
        }

        if (planoMensalClienteRepository.existsByCliente_id(cliente.getId())) {
            throw new BusinessException("Cliente ja possui um plano cadastrado.");
        }

        PlanosMensal plano = planoMensalRepository.findById(planoId)
                .orElseThrow(() -> new NotFoundException("Plano nao encontrado"));

        if (!Boolean.TRUE.equals(plano.getAtivo())) {
            throw new BusinessException("Plano selecionado esta desativado.");
        }

        PlanoMensalCliente planoMensalCliente = new PlanoMensalCliente();
        planoMensalCliente.setPlanoMensal(plano);
        planoMensalCliente.setCliente(cliente);
        planoMensalCliente.setUsuario(cliente.getUsuario());
        planoMensalCliente.setValorMensal(plano.getValorMensal());
        planoMensalCliente.setDiasMaximoAntecedencia(plano.getDiasMaximoAntecedencia());
        planoMensalCliente.setCriadoEm(LocalDateTime.now());
        planoMensalCliente.setAtualizadoEm(LocalDateTime.now());
        planoMensalCliente.setAtivo(true);

        return PlanoContratadoResponse.de(planoMensalClienteRepository.save(planoMensalCliente));
    }



    private List<PlanoMensalResponse> planosDisponiveis(PlanoContratadoResponse planoAtual) {
        return planoMensalRepository.findAllByAtivoTrueOrderByValorMensalAsc().stream()
                .filter(plano -> planoAtual == null || !plano.getId().equals(planoAtual.planoMensalId()))
                .map(PlanoMensalResponse::de)
                .toList();
    }
}
