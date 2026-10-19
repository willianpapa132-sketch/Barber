package bruninho.Barbeiro.service;

import bruninho.Barbeiro.Controller.Admin.cadastros.barbeiro.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.DayOfWeek;
import java.util.*;

@Service
public class CadastroBarbeiroAdminService {
    private final BarbeiroRepository barbeiros;
    private final UsuarioRepository usuarios;
    private final JornadaBarbeiroRepository jornadas;
    private final HorarioFuncionamentoRepository horarios;
    private final PasswordEncoder encoder;

    public CadastroBarbeiroAdminService(BarbeiroRepository barbeiros, UsuarioRepository usuarios,
                                       JornadaBarbeiroRepository jornadas, HorarioFuncionamentoRepository horarios,
                                       PasswordEncoder encoder) {
        this.barbeiros = barbeiros; this.usuarios = usuarios; this.jornadas = jornadas;
        this.horarios = horarios; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public BarbeiroDetalheResponse consultar(Long id) { return resposta(localizar(id)); }

    @Transactional
    public BarbeiroDetalheResponse salvar(Long id, SalvarBarbeiroRequest r) {
        var barbeiro = id == null ? new Barbeiro() : barbeiros.findByIdParaAgendar(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado"));
        var usuario = id == null ? new Usuario() : barbeiro.getUsuario();
        if (id != null && usuario.getRole() != ROLE.BARBEIRO) throw erro("Usuario vinculado nao possui perfil BARBEIRO");
        var login = r.login().trim().toLowerCase(Locale.ROOT);
        usuarios.findByLoginIgnoreCase(login).ifPresent(existente -> {
            if (!Objects.equals(existente.getId(), usuario.getId())) throw erro("Este e-mail ja esta cadastrado");
        });
        if (id == null && (r.senha() == null || r.senha().isBlank())) throw erro("Informe a senha do novo barbeiro");
        if (r.senha() != null && !r.senha().isBlank() && (r.senha().length() < 8 || r.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72))
            throw erro("Senha deve ter ao menos 8 caracteres e no maximo 72 bytes");
        validarFoto(r.imgUrl());
        var dias = EnumSet.noneOf(DayOfWeek.class);
        for (var dia : r.jornada()) {
            if (!dias.add(dia.diaSemana())) throw erro("Informe cada dia da semana uma unica vez");
            validarDia(dia);
        }
        usuario.setLogin(login); usuario.setRole(ROLE.BARBEIRO); usuario.setAtivo(r.ativo());
        if (r.senha() != null && !r.senha().isBlank()) usuario.setSenha(encoder.encode(r.senha()));
        usuarios.save(usuario);
        barbeiro.setUsuario(usuario); barbeiro.setNome(r.nome().trim()); barbeiro.setTelefone(r.telefone().trim());
        barbeiro.setPorcentagemComissao(r.comissao()); barbeiro.setAtivo(r.ativo()); barbeiro.setImgurl(r.imgUrl());
        barbeiros.save(barbeiro);
        for (var dia : r.jornada()) {
            var jornada = jornadas.findByDiaSemanaAndBarbeiro_id(dia.diaSemana(), barbeiro.getId()).orElseGet(JornadaBarbeiro::new);
            jornada.setBarbeiro(barbeiro); jornada.setDiaSemana(dia.diaSemana()); jornada.setAtivo(true); jornada.setFolga(dia.folga());
            jornada.setHoraInicio(dia.folga() ? null : dia.horaInicio()); jornada.setHoraFim(dia.folga() ? null : dia.horaFim());
            jornada.setIntervaloInicio(dia.folga() ? null : dia.intervaloInicio()); jornada.setIntervaloFim(dia.folga() ? null : dia.intervaloFim());
            jornadas.save(jornada);
        }
        return resposta(barbeiro);
    }

    private void validarDia(SalvarBarbeiroRequest.Dia d) {
        if (d.folga()) return;
        if (d.horaInicio() == null || d.horaFim() == null || !d.horaInicio().isBefore(d.horaFim()))
            throw erro("Informe inicio anterior ao fim da jornada em " + d.diaSemana());
        if ((d.intervaloInicio() == null) != (d.intervaloFim() == null)) throw erro("Informe os dois horarios do intervalo ou deixe ambos vazios");
        if (d.intervaloInicio() != null && (!d.intervaloInicio().isBefore(d.intervaloFim())
                || d.intervaloInicio().isBefore(d.horaInicio()) || d.intervaloFim().isAfter(d.horaFim())))
            throw erro("Intervalo deve estar dentro da jornada em " + d.diaSemana());
        var horario = horarios.findByDiaSemana(d.diaSemana());
        if (horario != null && (Boolean.TRUE.equals(horario.getFechado()) || horario.getHoraAbertura() == null
                || horario.getHoraFechamento() == null || d.horaInicio().isBefore(horario.getHoraAbertura())
                || d.horaFim().isAfter(horario.getHoraFechamento())))
            throw erro("Jornada deve respeitar o horario de funcionamento em " + d.diaSemana());
    }

    private void validarFoto(String foto) {
        if (foto == null || foto.isBlank()) return;
        if (!foto.startsWith("data:")) {
            if (foto.startsWith("https://") && foto.length() <= 2048) return;
            throw erro("Foto invalida");
        }
        var partes = foto.split(",", 2);
        if (partes.length != 2 || !partes[0].matches("data:image/(png|jpeg|webp);base64")) throw erro("Use uma foto PNG, JPG ou WEBP");
        try {
            byte[] bytes = Base64.getDecoder().decode(partes[1]);
            if (bytes.length > 2 * 1024 * 1024 || javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes)) == null)
                throw erro("Foto invalida ou maior que 2 MB");
        } catch (java.io.IOException | IllegalArgumentException e) { throw erro("Foto invalida"); }
    }

    private Barbeiro localizar(Long id) { return barbeiros.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Barbeiro nao encontrado")); }
    private BarbeiroDetalheResponse resposta(Barbeiro b) {
        var dias = Arrays.stream(DayOfWeek.values()).map(d -> jornadas.findByDiaSemanaAndBarbeiro_id(d, b.getId())
                .map(j -> new SalvarBarbeiroRequest.Dia(d, Boolean.TRUE.equals(j.getFolga()), j.getHoraInicio(), j.getHoraFim(), j.getIntervaloInicio(), j.getIntervaloFim()))
                .orElseGet(() -> new SalvarBarbeiroRequest.Dia(d, true, null, null, null, null))).toList();
        return new BarbeiroDetalheResponse(b.getId(), b.getNome(), b.getTelefone(), b.getUsuario().getLogin(), b.getPorcentagemComissao(), b.isAtivo(), b.getImgurl(), dias);
    }
    private ResponseStatusException erro(String mensagem) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem); }
}
