package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import bruninho.Barbeiro.Controller.Admin.configuracao.HorarioFuncionamentoBarber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.util.*;

@Service
@ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true",matchIfMissing=true)
public class PlatformCompanyService {
    public record Create(
            @NotBlank @Size(max=150)
            String nome,
            @NotBlank @Size(max=150)
            String responsavel,
            @Pattern(regexp="[a-z][a-z0-9-]{2,39}") @NotNull
            String slug,
            @NotBlank @Email @Size(max=120)
            String adminLogin,
            @NotBlank @Size(min=8,max=72)
            String adminPassword,
            @Size(max=30)String telefone,
            @Size(max=250)String endereco,
            @NotNull @Size(min=7,max=7)
            List<@NotNull @Valid HorarioFuncionamentoBarber> horarios
    ){}

    public record Company(
            UUID id,String nome,String responsavel,String slug,String host,String estado
    ){}

    public record Status(@NotNull Boolean ativa){}
    public record Administrator(Long id, String login) {}
    public record Credentials(@NotNull Long id, @NotBlank @Email @Size(max=120) String login,
                              @Size(max=72) String password) {}

    public List<Administrator> administrators(UUID empresaId) {
        registry.byId(empresaId);
        return registry.central().query("select id,login from usuario where empresa_id=? and role='ADMIN' order by id",
                (rs,n) -> new Administrator(rs.getLong("id"),rs.getString("login")),empresaId);
    }

    public void credentials(UUID empresaId, Credentials r) {
        registry.byId(empresaId);
        boolean changePassword = r.password()!=null && !r.password().isEmpty();
        if(changePassword && (r.password().length()<8 || r.password().getBytes(StandardCharsets.UTF_8).length>72))
            throw bad("Senha deve ter pelo menos 8 caracteres e no maximo 72 bytes");
        String login=r.login().trim().toLowerCase(Locale.ROOT);
        try {
            int count=changePassword
                    ? registry.central().update("update usuario set login=?,senha=? where id=? and empresa_id=? and role='ADMIN'",
                        login,encoder.encode(r.password()),r.id(),empresaId)
                    : registry.central().update("update usuario set login=? where id=? and empresa_id=? and role='ADMIN'",
                        login,r.id(),empresaId);
            if(count!=1)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Administrador nao encontrado nesta empresa");
        } catch(DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Este e-mail ja esta em uso nesta empresa");
        }
    }
    private final TenantRegistry registry;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public PlatformCompanyService(TenantRegistry registry,PasswordEncoder encoder,AppProperties props){
        this.registry=registry;
        this.encoder=encoder;
        this.props=props;
    }

    private Company response(TenantRegistry.Company c){
        return new Company(c.id(),c.nome(),c.responsavel(),c.slug(),c.slug()+"."+props.getMultiTenancy().getBaseDomain(),c.estado());
    }

    public List<Company> list(){
        return registry.all().stream().map(this::response).toList();
    }

    public Company create(Create r){
        var dias=EnumSet.noneOf(DayOfWeek.class);
        for(var h:r.horarios()){
            if(!dias.add(h.diaSemana()))throw bad("Informe cada dia da semana uma unica vez");
            if(!h.fechado()&&(h.horarioInicio()==null||h.horarioFim()==null||!h.horarioInicio().isBefore(h.horarioFim())))
                throw bad("Informe abertura anterior ao fechamento em "+h.diaSemana());
        }
        if(dias.size()!=7)throw bad("Informe os sete dias da semana");
        if(Set.of("www","admin","api","painel","platform","plataforma").contains(r.slug())
                ||(r.slug()+"."+props.getMultiTenancy().getBaseDomain()).equalsIgnoreCase(props.getMultiTenancy().getPlatformHost()))
            throw bad("Este endereco e reservado");
        if(r.adminPassword().getBytes(StandardCharsets.UTF_8).length>72)throw bad("Senha deve ter no maximo 72 bytes");
        UUID id=UUID.randomUUID();
        var jdbc=registry.central();
        // A criacao inteira e atomica no banco compartilhado, inclusive admin e horarios.
        var tx=new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
        try {
            tx.executeWithoutResult(status -> {
                jdbc.update("insert into platform_empresa(id,nome,responsavel,slug,estado,criado_em) values (?,?,?,?,'ATIVA',CURRENT_TIMESTAMP)",
                        id,r.nome().trim(),r.responsavel().trim(),r.slug());
                jdbc.update("insert into configuracao_barbearia(empresa_id,nome,telefone,endereco,dias_maximo_antecedentia,dias_tolerancia_pagamento_plano) values (?,?,?,?,30,0)",
                        id,r.nome().trim(),r.telefone()==null?null:r.telefone().trim(),r.endereco()==null?null:r.endereco().trim());
                jdbc.update("insert into usuario(empresa_id,login,senha,role,ativo) values (?,?,?,'ADMIN',true)",
                        id,r.adminLogin().trim().toLowerCase(Locale.ROOT),encoder.encode(r.adminPassword()));
                for(var h:r.horarios())
                    jdbc.update("insert into horario_funcionamento(empresa_id,dia_semana,hora_abertura,hora_fechamento,fechado,permite_agendamento_plano,intervalo_minimo_entre_agendamentos) values (?,?,?,?,?,?,?)",
                            id,h.diaSemana().name(),h.horarioInicio(),h.horarioFim(),h.fechado(),!h.fechado()&&h.permiteAgendamentoPlano(),h.intervaloMinimoEntreAgendamentos());
            });
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Este endereco ja foi cadastrado");
        }
        return response(registry.byId(id));
    }

    public Company status(UUID id,boolean active){
        var c=registry.byId(id);
        if(!Set.of("ATIVA","SUSPENSA").contains(c.estado()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Empresa ainda nao esta pronta para ativacao");
        registry.central().update("update platform_empresa set estado=? where id=?",active?"ATIVA":"SUSPENSA",id);
        return response(registry.byId(id));
    }

    private ResponseStatusException bad(String m){
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);
    }
}
