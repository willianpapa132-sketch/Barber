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
    private final bruninho.Barbeiro.repository.BarbeiroRepository barbeiros;
    private final CancelamentoPlanoClienteService cancelamento;

    public PlanoMensalClienteService(
            PlanoMensalRepository planoMensalRepository,
            ClienteRepository clienteRepository,
            PlanoMensalClienteRepository planoMensalClienteRepository,
            bruninho.Barbeiro.repository.BarbeiroRepository barbeiros,
            CancelamentoPlanoClienteService cancelamento
    ) {
        this.planoMensalRepository = planoMensalRepository;
        this.clienteRepository = clienteRepository;
        this.planoMensalClienteRepository = planoMensalClienteRepository;
        this.barbeiros = barbeiros;
        this.cancelamento=cancelamento;
    }

    @Transactional(readOnly = true)
    public PlanoClientePageResponse buscarTelaPlanos(String login) {
        var cliente = clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        var contrato = planoMensalClienteRepository.findByCliente_IdAndAtivoTrue(cliente.getId()).orElse(null);
        var planoAtual = contrato==null ? null : PlanoContratadoResponse.de(contrato);

        var planosDisponiveis = planosDisponiveis(planoAtual);

        return new PlanoClientePageResponse(
                planoAtual != null,
                planoAtual,
                planosDisponiveis,
                barbeiros.findAllByAtivoTrueOrderByNomeAsc().stream()
                        .map(bruninho.Barbeiro.Controller.cliente.home.agendamentos.BarbeiroAgendamentoResponse::de).toList(),
                contrato==null ? null : cancelamento.avaliar(contrato)
        );
    }

    @Transactional
    public PlanoContratadoResponse contratarPlano(String login, Long planoId) {
        return contratarPlano(login, planoId, null);
    }

    @Transactional
    public PlanoContratadoResponse contratarPlano(String login, Long planoId, Long barbeiroPreferidoId) {
        var cliente = clienteRepository.bloquearParaPlano(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));

        if (!Boolean.TRUE.equals(cliente.getUsuario().getAtivo())) {
            throw new BusinessException("Cliente esta desativado.");
        }

        if (planoMensalClienteRepository.findByCliente_IdAndAtivoTrue(cliente.getId()).isPresent()) {
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
        planoMensalCliente.setProximoVencimento(planoMensalCliente.getCriadoEm().toLocalDate());
        planoMensalCliente.setAtualizadoEm(LocalDateTime.now());
        planoMensalCliente.setAtivo(true);
        planoMensalCliente.setBarbeiroPreferido(barbeiroPreferido(barbeiroPreferidoId));

        return PlanoContratadoResponse.de(planoMensalClienteRepository.save(planoMensalCliente));
    }

    @Transactional
    public PlanoContratadoResponse atualizarPreferencia(String login, Long barbeiroId) {
        var cliente = clienteRepository.findByUsuario_Login(login)
                .orElseThrow(() -> new NotFoundException("Cliente nao encontrado"));
        var contrato = planoMensalClienteRepository.findByCliente_IdAndAtivoTrue(cliente.getId())
                .orElseThrow(() -> new NotFoundException("Plano ativo nao encontrado"));
        contrato.setBarbeiroPreferido(barbeiroPreferido(barbeiroId));
        contrato.setAtualizadoEm(LocalDateTime.now());
        return PlanoContratadoResponse.de(contrato);
    }

    private bruninho.Barbeiro.domain.Barbeiro barbeiroPreferido(Long id) {
        if (id == null) return null;
        var barbeiro = barbeiros.findById(id).orElseThrow(() -> new NotFoundException("Barbeiro nao encontrado"));
        if (!barbeiro.isAtivo()) throw new BusinessException("Escolha um barbeiro ativo para sua preferencia.");
        return barbeiro;
    }



    private List<PlanoMensalResponse> planosDisponiveis(PlanoContratadoResponse planoAtual) {
        return planoMensalRepository.findAllByAtivoTrueOrderByValorMensalAsc().stream()
                .filter(plano -> planoAtual == null || !plano.getId().equals(planoAtual.planoMensalId()))
                .map(PlanoMensalResponse::de)
                .toList();
    }
}
