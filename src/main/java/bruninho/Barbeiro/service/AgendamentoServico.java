package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Agendamento;
import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.domain.PlanoMensalCliente;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.StatusAgendamento;
import bruninho.Barbeiro.domain.Usuario;
import bruninho.Barbeiro.repository.AgendamentoRepositorio;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ClienteRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.web.form.AgendamentoForm;
import bruninho.Barbeiro.web.form.AgendamentoPublicoForm;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgendamentoServico {
    private final AgendamentoRepositorio agendamentos;
    private final BarbeiroRepositorio barbeiros;
    private final ServicoRepositorio servicos;
    private final ClienteRepositorio clientes;
    private final DisponibilidadeServico disponibilidade;
    private final NormalizadorTelefone normalizadorTelefone;
    private final UsuarioAtualServico usuarioAtual;
    private final PlanoMensalServico planosMensais;
    private final SecureRandom random = new SecureRandom();

    public AgendamentoServico(AgendamentoRepositorio agendamentos, BarbeiroRepositorio barbeiros, ServicoRepositorio servicos,
                              ClienteRepositorio clientes, DisponibilidadeServico disponibilidade, NormalizadorTelefone normalizadorTelefone,
                              UsuarioAtualServico usuarioAtual, PlanoMensalServico planosMensais) {
        this.agendamentos = agendamentos;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
        this.clientes = clientes;
        this.disponibilidade = disponibilidade;
        this.normalizadorTelefone = normalizadorTelefone;
        this.usuarioAtual = usuarioAtual;
        this.planosMensais = planosMensais;
    }

    @Transactional
    public Agendamento createPublic(AgendamentoPublicoForm form) {
        normalizadorTelefone.validate(form.getTelefoneCliente());
        return create(form.getServicoId(), form.getBarbeiroId(), LocalDateTime.of(form.getData(), form.getHora()),
            form.getNomeCliente(), form.getTelefoneCliente(), null, null);
    }

    @Transactional
    public Agendamento createManual(AgendamentoForm form) {
        normalizadorTelefone.validate(form.getTelefoneCliente());
        Cliente cliente = null;
        if (form.getClienteId() != null) {
            cliente = clientes.findById(form.getClienteId()).orElseThrow(() -> new RegraNegocioException("Cliente não encontrado."));
        }
        return create(form.getServicoId(), form.getBarbeiroId(), LocalDateTime.of(form.getData(), form.getHora()),
            form.getNomeCliente(), form.getTelefoneCliente(), cliente, usuarioAtual.requiredUser());
    }

    private Agendamento create(Long servicoId, Long barbeiroId, LocalDateTime inicioEm, String nomeCliente, String telefoneCliente, Cliente cliente, Usuario ator) {
        Barbeiro barbeiro = barbeiros.lockById(barbeiroId).orElseThrow(() -> new RegraNegocioException("Barbeiro não encontrado."));
        Servico servico = servicos.findById(servicoId).orElseThrow(() -> new RegraNegocioException("Serviço não encontrado."));
        disponibilidade.assertAvailable(servicoId, barbeiro.getId(), inicioEm, null);
        PlanoMensalCliente planoMensal = planosMensais.planoAtivoPara(telefoneCliente, barbeiro.getId(), inicioEm.toLocalDate());
        if (planoMensal != null) {
            planosMensais.validarUsoDisponivel(planoMensal, inicioEm);
            cliente = planoMensal.getCliente();
        }
        Agendamento agendamento = new Agendamento();
        agendamento.setCodigoConfirmacao(newCode());
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setPlanoMensal(planoMensal);
        agendamento.setCliente(cliente);
        agendamento.setNomeCliente(nomeCliente.trim());
        agendamento.setTelefoneCliente(telefoneCliente.trim());
        agendamento.setNomeServicoSnapshot(servico.getNome());
        agendamento.setPrecoServicoSnapshot(servico.getPreco());
        agendamento.setDuracaoServicoMinutosSnapshot(servico.getDuracaoMinutos());
        agendamento.setInicioEm(inicioEm);
        agendamento.setFimEm(inicioEm.plusMinutes(servico.getDuracaoMinutos()));
        agendamento.setCriadoPorUsuario(ator);
        return agendamentos.save(agendamento);
    }

    @Transactional
    public Agendamento reschedule(Long agendamentoId, Long barbeiroId, Long servicoId, LocalDateTime inicioEm) {
        Agendamento agendamento = agendamentos.findById(agendamentoId).orElseThrow();
        if (agendamento.getStatus() != StatusAgendamento.AGENDADO) {
            throw new RegraNegocioException("Somente agendamentos em aberto podem ser remarcados.");
        }
        Barbeiro barbeiro = barbeiros.lockById(barbeiroId).orElseThrow();
        Servico servico = servicos.findById(servicoId).orElseThrow();
        disponibilidade.assertAvailable(servicoId, barbeiroId, inicioEm, agendamentoId);
        agendamento.setBarbeiro(barbeiro);
        agendamento.setServico(servico);
        agendamento.setNomeServicoSnapshot(servico.getNome());
        agendamento.setPrecoServicoSnapshot(servico.getPreco());
        agendamento.setDuracaoServicoMinutosSnapshot(servico.getDuracaoMinutos());
        agendamento.setInicioEm(inicioEm);
        agendamento.setFimEm(inicioEm.plusMinutes(servico.getDuracaoMinutos()));
        agendamento.touch();
        return agendamento;
    }

    @Transactional
    public void changeStatus(Long id, StatusAgendamento target, String reason) {
        Agendamento agendamento = agendamentos.findById(id).orElseThrow();
        StatusAgendamento current = agendamento.getStatus();
        boolean allowed = current == StatusAgendamento.AGENDADO &&
            (target == StatusAgendamento.CONCLUIDO || target == StatusAgendamento.CANCELADO || target == StatusAgendamento.NAO_COMPARECEU);
        if (!allowed) throw new RegraNegocioException("Transição de status não permitida.");
        agendamento.setStatus(target);
        agendamento.setMotivoCancelamento(reason);
        agendamento.touch();
    }

    private String newCode() {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
