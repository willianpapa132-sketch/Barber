package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import bruninho.Barbeiro.Controller.Admin.configuracao.HorarioFuncionamentoBarber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.util.*;

@Service
@ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true")
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
    private final TenantRegistry registry;
    private final TenantSecrets secrets;
    private final PasswordEncoder encoder;
    private final AppProperties props;

    public PlatformCompanyService(TenantRegistry registry,TenantSecrets secrets,PasswordEncoder encoder,AppProperties props){
        this.registry=registry;
        this.secrets=secrets;
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
        UUID id=UUID.randomUUID();String suffix=id.toString().replace("-","").substring(0,24);
        String db="barber_t_"+suffix,user="barber_u_"+suffix;
        byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String password=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        try{registry.central().update("insert into platform_empresa(id,nome,responsavel,slug,db_name,db_user,db_password,estado) values (?,?,?,?,?,?,?,'CRIANDO')",id,r.nome().trim(),r.responsavel().trim(),r.slug(),db,user,secrets.encrypt(password));}
        catch(DuplicateKeyException e){throw new ResponseStatusException(HttpStatus.CONFLICT,"Este endereco ja foi cadastrado");}
        boolean roleCreated=false,dbCreated=false;
        try{
            // Identificadores e senha gerados pelo servidor, nunca recebidos do navegador.
            registry.central().execute("create role "+user+" login password '"+password+"' nosuperuser nocreatedb nocreaterole noinherit");roleCreated=true;
            registry.central().execute("create database "+db+" owner "+user);dbCreated=true;
            registry.central().execute("revoke connect on database "+db+" from public");
            var company=registry.byId(id);var jdbc=new JdbcTemplate(registry.dataSource(company));
            var tx=new org.springframework.transaction.support.TransactionTemplate(new org.springframework.jdbc.datasource.DataSourceTransactionManager(jdbc.getDataSource()));
            tx.executeWithoutResult(status->{
                jdbc.update("insert into configuracao_barbearia(id,nome,telefone,endereco,dias_maximo_antecedentia,dias_tolerancia_pagamento_plano) values (1,?,?,?,30,0)",r.nome().trim(),r.telefone()==null?null:r.telefone().trim(),r.endereco()==null?null:r.endereco().trim());
                jdbc.update("insert into usuario(login,senha,role,ativo) values (?,?,'ADMIN',true)",r.adminLogin().trim().toLowerCase(Locale.ROOT),encoder.encode(r.adminPassword()));
                for(var h:r.horarios())jdbc.update("insert into horario_funcionamento(dia_semana,hora_abertura,hora_fechamento,fechado,permite_agendamento_plano,intervalo_minimo_entre_agendamentos) values (?,?,?,?,?,?)",h.diaSemana().name(),h.horarioInicio(),h.horarioFim(),h.fechado(),!h.fechado()&&h.permiteAgendamentoPlano(),h.intervaloMinimoEntreAgendamentos());
            });
            registry.central().update("update platform_empresa set estado='ATIVA' where id=?",id);
            return response(registry.byId(id));
        }catch(RuntimeException e){
            registry.evict(id);
            try{if(dbCreated)registry.central().execute("drop database "+db+" with (force)");if(roleCreated)registry.central().execute("drop role "+user);}catch(RuntimeException ignored){/* Estado FALHOU permanece visivel para manutencao. */}
            registry.central().update("update platform_empresa set estado='FALHOU' where id=?",id);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Nao foi possivel preparar a empresa. Confira as permissoes de provisionamento e a disponibilidade do PostgreSQL.");
        }
    }

    public Company status(UUID id,boolean active){
        var c=registry.byId(id);
        if(!Set.of("ATIVA","SUSPENSA").contains(c.estado()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Empresa ainda nao esta pronta para ativacao");
        registry.central().update("update platform_empresa set estado=? where id=?",active?"ATIVA":"SUSPENSA",id);registry.evict(id);
        return response(registry.byId(id));
    }

    private ResponseStatusException bad(String m){
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);
    }
}
