package bruninho.Barbeiro.service;

import bruninho.Barbeiro.domain.Barbeiro;
import bruninho.Barbeiro.domain.Cliente;
import bruninho.Barbeiro.domain.JornadaBarbeiro;
import bruninho.Barbeiro.domain.Perfil;
import bruninho.Barbeiro.domain.Servico;
import bruninho.Barbeiro.domain.Usuario;
import bruninho.Barbeiro.repository.BarbeiroRepositorio;
import bruninho.Barbeiro.repository.ClienteRepositorio;
import bruninho.Barbeiro.repository.JornadaBarbeiroRepositorio;
import bruninho.Barbeiro.repository.ServicoRepositorio;
import bruninho.Barbeiro.repository.UsuarioRepositorio;
import bruninho.Barbeiro.web.form.BarbeiroForm;
import bruninho.Barbeiro.web.form.ClienteForm;
import bruninho.Barbeiro.web.form.ServicoForm;
import java.time.LocalTime;
import java.util.HashSet;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import bruninho.Barbeiro.service.NormalizadorTelefone;

@Service
public class AdminCatalogoServico {
    private final ServicoRepositorio servicos;
    private final BarbeiroRepositorio barbeiros;
    private final UsuarioRepositorio usuarios;
    private final JornadaBarbeiroRepositorio jornadas;
    private final ClienteRepositorio clientes;
    private final NormalizadorTelefone telefones;
    private final PasswordEncoder encoder;

    public AdminCatalogoServico(ServicoRepositorio servicos, BarbeiroRepositorio barbeiros, UsuarioRepositorio usuarios,
                               JornadaBarbeiroRepositorio jornadas, ClienteRepositorio clientes,
                               NormalizadorTelefone telefones, PasswordEncoder encoder) {
        this.servicos = servicos;
        this.barbeiros = barbeiros;
        this.usuarios = usuarios;
        this.jornadas = jornadas;
        this.clientes = clientes;
        this.telefones = telefones;
        this.encoder = encoder;
    }

    @Transactional
    public Servico saveService(Long id, ServicoForm form) {
        Servico servico = id == null ? new Servico() : servicos.findById(id).orElseThrow();
        servico.setNome(form.getNome().trim());
        servico.setDescricao(form.getDescricao());
        servico.setPreco(form.getPreco());
        servico.setDuracaoMinutos(form.getDuracaoMinutos());
        servico.setAtivo(form.isAtivo());
        servico.touch();
        return servicos.save(servico);
    }

    @Transactional
    public Barbeiro saveBarbeiro(Long id, BarbeiroForm form) {
        telefones.validate(form.getTelefone());
        Barbeiro barbeiro = id == null ? new Barbeiro() : barbeiros.findById(id).orElseThrow();
        Usuario usuario = id == null ? new Usuario() : barbeiro.getUsuario();
        if (id == null && usuarios.existsByLogin(form.getLogin())) throw new RegraNegocioException("Usuário já existe.");
        usuario.setLogin(form.getLogin().trim());
        usuario.setNome(form.getNome().trim());
        usuario.setPerfil(Perfil.BARBEIRO);
        usuario.setAtivo(form.isAtivo());
        if (id == null || (form.getPassword() != null && !form.getPassword().isBlank())) {
            usuario.setSenhaHash(encoder.encode(form.getPassword()));
        }
        usuario.touch();
        usuarios.save(usuario);
        barbeiro.setNome(form.getNome().trim());
        barbeiro.setTelefone(form.getTelefone());
        barbeiro.setAtivo(form.isAtivo());
        barbeiro.setUsuario(usuario);
        barbeiro.getServicos().clear();
        barbeiro.getServicos().addAll(new HashSet<>(servicos.findAllById(form.getServicoIds())));
        return barbeiros.save(barbeiro);
    }

    @Transactional
    public void defaultSchedule(Long barbeiroId) {
        Barbeiro barbeiro = barbeiros.findById(barbeiroId).orElseThrow();
        jornadas.deleteByBarbeiroId(barbeiroId);
        for (int d = 1; d <= 6; d++) {
            JornadaBarbeiro jornada = new JornadaBarbeiro();
            jornada.setBarbeiro(barbeiro);
            jornada.setDiaSemana(d);
            jornada.setHoraInicio(LocalTime.of(9, 0));
            jornada.setHoraFim(d == 6 ? LocalTime.of(14, 0) : LocalTime.of(18, 0));
            jornada.setIntervaloInicio(LocalTime.of(12, 0));
            jornada.setIntervaloFim(LocalTime.of(13, 0));
            jornadas.save(jornada);
        }
    }

    @Transactional
    public Cliente saveCliente(Long id, ClienteForm form) {
        telefones.validate(form.getTelefone());
        Cliente c = id == null ? new Cliente() : clientes.findById(id).orElseThrow();
        c.setNome(form.getNome().trim());
        c.setTelefone(form.getTelefone().trim());
        c.setTelefoneNormalizado(telefones.normalize(form.getTelefone()));
        c.touch();
        return clientes.save(c);
    }
}
