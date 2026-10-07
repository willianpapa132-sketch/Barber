package bruninho.Barbeiro.service;

import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.HorarioIndisponivelException;

import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.ROLE;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ValidacaoAgendamentoService {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final BarbeiroRepository barbeiros;
    private final ServicoRepository servicos;

    public ValidacaoAgendamentoService(UsuarioRepository usuarios, ClienteRepository clientes,
                                      BarbeiroRepository barbeiros, ServicoRepository servicos) {
        this.usuarios = usuarios;
        this.clientes = clientes;
        this.barbeiros = barbeiros;
        this.servicos = servicos;
    }

    public Cliente validarCliente(Long usuarioId, Long clienteId) {
        validarId(usuarioId);
        validarId(clienteId);
        var usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NotFoundException("Usuário não encontrado"));
        var cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new NotFoundException("Cliente não encontrado"));
        if (cliente.getUsuario() == null || !usuarioId.equals(cliente.getUsuario().getId()))
            throw new BusinessException("O usuário informado não pertence ao cliente");
        if (usuario.getRole() != ROLE.CLIENTE || !Boolean.TRUE.equals(usuario.getAtivo()))
            throw new BusinessException("O usuário deve ser um cliente ativo");
        return cliente;
    }

    public Barbeiro validarBarbeiro(Long id) {
        validarId(id);
        var barbeiro = barbeiros.findById(id)
                .orElseThrow(() -> new NotFoundException("Barbeiro não encontrado"));
        validarBarbeiroAtivo(barbeiro);
        return barbeiro;
    }

    public void validarBarbeiroAtivo(Barbeiro barbeiro) {
        if (!barbeiro.isAtivo()) throw new BusinessException("Barbeiro inativo");
    }

    public Set<Servico> validarServicos(List<Long> ids) {
        if (ids == null || ids.isEmpty() || new HashSet<>(ids).size() != ids.size())
            throw new BusinessException("Informe serviços sem repetições");
        Set<Servico> resultado = new LinkedHashSet<>();
        long duracao = 0;
        for (Long id : ids) {
            validarId(id);
            var servico = servicos.findById(id)
                    .orElseThrow(() -> new NotFoundException("Serviço não encontrado: " + id));
            if (!servico.isAtivo() || servico.getPreco() == null || servico.getPreco().signum() < 0
                    || servico.getDuracaoMinutos() == null || servico.getDuracaoMinutos() <= 0)
                throw new BusinessException("Serviço inativo ou com preço/duração inválidos: " + id);
            duracao += servico.getDuracaoMinutos();
            resultado.add(servico);
        }
        if (duracao >= 1440) throw new BusinessException("A duração deve ser inferior a um dia");
        return resultado;
    }

    public void validaAgendamento(Agendamento agendamento, DisponibilidadeAgendamentosService disponibilidade) {
        if (agendamento.getData() == null || agendamento.getHoraInicio() == null)
            throw new BusinessException("Informe a data e a hora de início");
        if (!LocalDateTime.of(agendamento.getData(), agendamento.getHoraInicio()).isAfter(LocalDateTime.now()))
            throw new BusinessException("O agendamento deve ser no futuro");
        if (!disponibilidade.horariosDisponiveis(agendamento.getCliente().getUsuario(),
                agendamento.getBarbeiro().getId(), agendamento.getData(), agendamento.getServico())
                .contains(agendamento.getHoraInicio()))
            throw new HorarioIndisponivelException("Horário indisponível");
    }

    public void validarId(Long id) {
        if (id == null || id <= 0) throw new BusinessException("Os IDs devem ser positivos");
    }
}
