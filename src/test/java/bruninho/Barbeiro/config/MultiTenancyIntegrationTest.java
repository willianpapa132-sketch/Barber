package bruninho.Barbeiro.config;

import bruninho.Barbeiro.platform.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import tools.jackson.databind.json.JsonMapper;
import java.util.UUID;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named="PLATFORM_TEST_DB_URL",matches="jdbc:postgresql:.*")
@SpringBootTest(properties={"app.multi-tenancy.enabled=true","app.multi-tenancy.platform-host=localhost",
        "app.multi-tenancy.base-domain=localhost","app.multi-tenancy.owner-login=dono@platform.test",
        "app.multi-tenancy.owner-password=Platform-test-12345",
        "app.multi-tenancy.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "spring.jpa.hibernate.ddl-auto=validate","spring.flyway.enabled=true","app.demo-data=false"})
@AutoConfigureMockMvc
class MultiTenancyIntegrationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->System.getenv("PLATFORM_TEST_DB_URL"));
        r.add("spring.datasource.username",()->System.getenv("PLATFORM_TEST_DB_USERNAME"));
        r.add("spring.datasource.password",()->System.getenv("PLATFORM_TEST_DB_PASSWORD"));
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");
    }
    @Autowired MockMvc mvc;
    @Autowired TenantRegistry registry;
    @Autowired TenantSecrets secrets;
    @Autowired TenantRoutingDataSource routing;
    @Autowired org.springframework.context.ApplicationContext context;
    private final JsonMapper json=new JsonMapper();
    private RequestPostProcessor host(String host){return request->{request.setServerName(host);return request;};}
    private MockHttpSession login(String host,String username,String password)throws Exception{
        var result=mvc.perform(post("/login").with(host(host)).with(csrf()).param("username",username).param("password",password))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession)result.getRequest().getSession(false);
    }
    @Test void provisionaBancosDistintosRoteiaLoginERecusaAcessoEntreEmpresas()throws Exception{
        var owner=login("localhost","dono@platform.test","Platform-test-12345");
        assertThat(context.containsBean("bootstrap")).isFalse();
        assertThat(registry.central().queryForObject("select count(*) from usuario",Long.class)).isZero();
        assertThat(registry.central().queryForObject("select count(*) from configuracao_barbearia",Long.class)).isZero();
        mvc.perform(get("/api/plataforma/csrf").with(host("localhost")).session(owner)).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(post("/api/plataforma/empresas").with(host("localhost")).session(owner).contentType("application/json").content(company("insegura")))
                .andExpect(status().isForbidden());
        var a=create(owner,"alfa");var b=create(owner,"beta");
        var jdbcA=new org.springframework.jdbc.core.JdbcTemplate(registry.dataSource(a));
        assertThat(jdbcA.queryForObject("select count(*) from usuario",Long.class)).isEqualTo(1);
        assertThat(jdbcA.queryForObject("select count(*) from horario_funcionamento",Long.class)).isEqualTo(7);
        assertThat(jdbcA.queryForObject("select count(*) from configuracao_barbearia",Long.class)).isEqualTo(1);
        for(var table:java.util.List.of("barbeiro","cliente","servico","plano_mensal","plano_mensal_cliente","agendamento","caixa","movimento_caixa","jornada_barbeiro"))
            assertThat(jdbcA.queryForObject("select count(*) from "+table,Long.class)).as(table).isZero();
        assertThat(jdbcA.queryForObject("select endereco from configuracao_barbearia where id=1",String.class)).isEqualTo("Rua da empresa, 10");
        assertThat(jdbcA.queryForObject("select telefone from configuracao_barbearia where id=1",String.class)).isEqualTo("11999990000");
        assertThat(jdbcA.queryForObject("select intervalo_minimo_entre_agendamentos from horario_funcionamento where dia_semana='MONDAY'",Integer.class)).isEqualTo(20);
        assertThat(a.dbName()).isNotEqualTo(b.dbName());assertThat(a.dbUser()).isNotEqualTo(b.dbUser());
        assertThat(a.encryptedPassword()).doesNotContain(secrets.decrypt(a.encryptedPassword()));
        var sessionA=login("alfa.localhost","admin@empresa.test","Empresa-test-12345");
        var sessionB=login("beta.localhost","admin@empresa.test","Empresa-test-12345");
        assertThat(sessionA.getAttribute(TenantContext.SESSION_SCOPE)).isEqualTo(a.id().toString());
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sessionA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa alfa"));
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sessionB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa beta"));
        var config=mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sessionA)).andReturn();
        var changed=(tools.jackson.databind.node.ObjectNode)json.readTree(config.getResponse().getContentAsString());changed.put("nome","Alfa alterada");
        mvc.perform(put("/api/admin/configuracao").with(host("alfa.localhost")).session(sessionA).contentType("application/json").content(json.writeValueAsString(changed)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Alfa alterada"));
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sessionB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa beta"));
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sessionA)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(owner)).andExpect(status().isForbidden());
        mvc.perform(get("/api/plataforma/empresas").with(host("alfa.localhost")).session(sessionA)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("desconhecida.localhost"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/configuracao").with(host("localhost")).header("X-Forwarded-Host","alfa.localhost").session(owner)).andExpect(status().isNotFound());
        mvc.perform(get("/api/plataforma/empresas").with(host("localhost")).session(owner))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].dbName").doesNotExist())
                .andExpect(jsonPath("$[0].encryptedPassword").doesNotExist());
        mvc.perform(post("/api/plataforma/empresas").with(host("localhost")).session(owner).with(csrf()).contentType("application/json").content(company("alfa")))
                .andExpect(status().isConflict());
        setStatus(owner,a.id(),false);
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sessionA)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sessionB)).andExpect(status().isOk());
        setStatus(owner,a.id(),true);
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sessionA)).andExpect(status().isOk());
        mvc.perform(post("/login").with(host("localhost")).with(csrf()).param("username","admin@empresa.test").param("password","Empresa-test-12345"))
                .andExpect(status().isUnauthorized());
        assertThat(TenantContext.get()).isNull();
        assertThatThrownBy(()->routing.getConnection()).isInstanceOf(IllegalStateException.class);
        var ds=registry.dataSource(a);
        try(var con=ds.getConnection();var stmt=con.createStatement();var rs=stmt.executeQuery("select rolsuper,rolcreatedb from pg_roles where rolname=current_user")){
            assertThat(rs.next()).isTrue();assertThat(rs.getBoolean(1)).isFalse();assertThat(rs.getBoolean(2)).isFalse();
        }
        assertThatThrownBy(()->{try(var ignored=DriverManager.getConnection(registry.url(b.dbName()),a.dbUser(),secrets.decrypt(a.encryptedPassword()))) {}})
                .isInstanceOf(java.sql.SQLException.class);
        // Reinicia uma segunda instancia contra o mesmo cadastro central e os mesmos bancos.
        try(var restarted=new org.springframework.boot.builder.SpringApplicationBuilder(bruninho.Barbeiro.BarbeiroApplication.class).run(
                "--server.port=0","--spring.datasource.url="+System.getenv("PLATFORM_TEST_DB_URL"),
                "--spring.datasource.username="+System.getenv("PLATFORM_TEST_DB_USERNAME"),
                "--spring.datasource.password="+System.getenv("PLATFORM_TEST_DB_PASSWORD"),
                "--app.multi-tenancy.enabled=true","--app.multi-tenancy.owner-login=dono@platform.test",
                "--app.multi-tenancy.owner-password=Platform-test-12345",
                "--app.multi-tenancy.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=","--app.demo-data=false")){
            var restored=restarted.getBean(TenantRegistry.class);
            var company=restored.find("alfa").orElseThrow();
            var jdbc=new org.springframework.jdbc.core.JdbcTemplate(restored.dataSource(company));
            assertThat(jdbc.queryForObject("select nome from configuracao_barbearia where id=1",String.class)).isEqualTo("Alfa alterada");
            assertThat(restored.all()).hasSize(2);
        }
    }
    private TenantRegistry.Company create(MockHttpSession session,String slug)throws Exception{
        mvc.perform(post("/api/plataforma/empresas").with(host("localhost")).session(session).with(csrf()).contentType("application/json").content(company(slug)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ATIVA"));
        return registry.find(slug).orElseThrow();
    }
    private void setStatus(MockHttpSession session,UUID id,boolean active)throws Exception{
        mvc.perform(put("/api/plataforma/empresas/"+id+"/status").with(host("localhost")).session(session).with(csrf())
                .contentType("application/json").content("{\"ativa\":"+active+"}")).andExpect(status().isOk());
    }
    private String company(String slug){
        var horarios=java.util.Arrays.stream(java.time.DayOfWeek.values()).map(d->java.util.Map.of(
                "diaSemana",d.name(),"horarioInicio","08:30","horarioFim","17:30","fechado",d==java.time.DayOfWeek.SUNDAY,
                "intervaloMinimoEntreAgendamentos",20,"permiteAgendamentoPlano",d!=java.time.DayOfWeek.SUNDAY)).toList();
        return json.writeValueAsString(java.util.Map.of("nome","Empresa "+slug,"responsavel","Responsavel","slug",slug,
                "adminLogin","admin@empresa.test","adminPassword","Empresa-test-12345","telefone","11999990000","endereco","Rua da empresa, 10","horarios",horarios));
    }
}
