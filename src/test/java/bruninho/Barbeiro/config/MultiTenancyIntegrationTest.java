package bruninho.Barbeiro.config;

import bruninho.Barbeiro.platform.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.util.*;
import java.math.BigDecimal;
import java.time.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.multi-tenancy.enabled=true", "app.multi-tenancy.platform-host=localhost",
        "app.multi-tenancy.base-domain=localhost", "app.multi-tenancy.owner-login=dono@platform.test",
        "app.multi-tenancy.owner-password=Platform-test-12345", "app.demo-data=false",
        "spring.datasource.url=jdbc:h2:mem:shared_tenants;DATABASE_TO_LOWER=TRUE",
        "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:platform-admin.sql"})
@ActiveProfiles("test") @AutoConfigureMockMvc
class MultiTenancyIntegrationTest {
    @org.springframework.test.context.DynamicPropertySource
    static void postgres(org.springframework.test.context.DynamicPropertyRegistry r) {
        if (System.getenv("PLATFORM_TEST_DB_URL") == null) return;
        r.add("spring.datasource.url", () -> System.getenv("PLATFORM_TEST_DB_URL"));
        r.add("spring.datasource.username", () -> System.getenv("PLATFORM_TEST_DB_USERNAME"));
        r.add("spring.datasource.password", () -> System.getenv("PLATFORM_TEST_DB_PASSWORD"));
        r.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        r.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.sql.init.mode", () -> "never");
    }
    @Autowired MockMvc mvc;
    @Autowired TenantRegistry registry;
    @Autowired org.springframework.context.ApplicationContext context;
    @Autowired TransactionTemplate tx;
    @Autowired ServicoRepository servicos;
    @Autowired ComandaRepository comandas;
    @Autowired BarbeiroRepository barbeiros;
    @Autowired ClienteRepository clientes;
    @Autowired AgendamentoRepository agendamentos;
    @Autowired CaixaRepository caixas;
    @Autowired MovimentoCaixaRepository movimentos;
    @Autowired JornadaBarbeiroRepository jornadas;
    @Autowired BloqueioBarbeiroRepository bloqueios;
    @Autowired PlanoMensalRepository planos;
    @Autowired PlanoMensalClienteRepository contratos;
    @Autowired RecebimentoPlanoRepository recebimentos;
    @Autowired ComissaoPlanoRepository pontos;
    @Autowired ComissaoBarbeiroRepository comissoes;
    @Autowired ServicosPlanosDescontoRepository descontos;
    @Autowired HorarioFuncionamentoRepository horarios;
    @Autowired ConfiguracaoBarbeariaRepository configuracoes;
    @Autowired bruninho.Barbeiro.security.repository.UsuarioRepository usuarios;
    private final JsonMapper json = new JsonMapper();
    private RequestPostProcessor host(String value) { return r -> { r.setServerName(value); return r; }; }
    private MockHttpSession login(String host,String username,String password) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").with(host(host)).with(csrf())
                .param("username",username).param("password",password)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    private MockHttpSession owner() throws Exception { return login("localhost","dono@platform.test","Platform-test-12345"); }
    private TenantRegistry.Company create(MockHttpSession owner,String slug) throws Exception {
        var dias=Arrays.stream(DayOfWeek.values()).map(d -> Map.of("diaSemana",d.name(),"horarioInicio","08:30",
                "horarioFim","17:30","fechado",false,"intervaloMinimoEntreAgendamentos",20,"permiteAgendamentoPlano",true)).toList();
        mvc.perform(post("/api/plataforma/empresas").with(host("localhost")).session(owner).with(csrf())
                .contentType("application/json").content(json.writeValueAsString(Map.of("nome","Empresa "+slug,
                        "responsavel","Responsavel","slug",slug,"adminLogin","admin@empresa.test",
                        "adminPassword","Empresa-test-12345","horarios",dias))))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("ATIVA"));
        return registry.find(slug).orElseThrow();
    }
    private <T> T empresa(UUID id, java.util.function.Supplier<T> action) {
        TenantContext.setEmpresaId(id);
        try { return tx.execute(status -> action.get()); } finally { TenantContext.clear(); }
    }
    private Servico servico(String nome) {
        var s=new Servico();s.setNome(nome);s.setPreco(BigDecimal.TEN);s.setDuracaoMinutos(30);return servicos.saveAndFlush(s);
    }
    @AfterEach void clear() { TenantContext.clear(); }

    @Test void bancoUnicoIsolaLoginCrudConfiguracaoESessao() throws Exception {
        var owner=owner();var a=create(owner,"alfa");var b=create(owner,"beta");
        assertThat(context.getBeansOfType(javax.sql.DataSource.class)).hasSize(1);
        assertThat(context.containsBean("bootstrap")).isTrue(); // Inicializador de testes nao insere dados neste modo.
        mvc.perform(post("/api/plataforma/empresas").with(host("localhost")).session(owner)
                .contentType("application/json").content("{}" )).andExpect(status().isForbidden());
        var sa=login("alfa.localhost","admin@empresa.test","Empresa-test-12345");
        var sb=login("beta.localhost","admin@empresa.test","Empresa-test-12345");
        assertThat(sa.getAttribute(TenantContext.SESSION_SCOPE)).isEqualTo(a.id().toString());
        var servicoA=empresa(a.id(),()->servico("Alfa"));
        var servicoB=empresa(b.id(),()->servico("Beta"));
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sa))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa alfa"));
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sb))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa beta"));
        var config=mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sa)).andReturn();
        var changed=(tools.jackson.databind.node.ObjectNode)json.readTree(config.getResponse().getContentAsString());
        changed.put("nome","Alfa alterada");
        mvc.perform(put("/api/admin/configuracao").with(host("alfa.localhost")).session(sa).contentType("application/json")
                .content(json.writeValueAsString(changed))).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Alfa alterada"));
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sb))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Empresa beta"));
        mvc.perform(get("/api/admin/servicos").param("empresaId",b.id().toString()).with(host("alfa.localhost")).session(sa))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.itens[0].nome").value("Alfa"));
        mvc.perform(get("/api/admin/servicos/"+servicoB.getId()).with(host("alfa.localhost")).session(sa)).andExpect(status().isNotFound());
        mvc.perform(put("/api/admin/servicos/"+servicoB.getId()).with(host("alfa.localhost")).session(sa)
                .contentType("application/json").content("{\"nome\":\"Invasao\",\"valor\":10,\"duracaoMinutos\":30,\"ativo\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/servicos/"+servicoB.getId()).with(host("alfa.localhost")).session(sa)).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/servicos").with(host("alfa.localhost")).session(sa).contentType("application/json")
                .content("{\"nome\":\"Novo alfa\",\"valor\":10,\"duracaoMinutos\":30,\"ativo\":true,\"empresaId\":\""+b.id()+"\"}"))
                .andExpect(status().isCreated());
        assertThat(empresa(b.id(),()->servicos.count())).isEqualTo(1);
        assertThat(empresa(a.id(),()->servicos.count())).isEqualTo(2);
        assertThat(empresa(a.id(),()->servicos.existsById(servicoB.getId()))).isFalse();
        assertThat(empresa(a.id(),()->servicos.findAllById(List.of(servicoA.getId(),servicoB.getId())))).hasSize(1);
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sa)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(owner)).andExpect(status().isForbidden());
        for(var path:List.of("/plataforma","/api/plataforma","/api/plataforma/empresas"))
            mvc.perform(get(path).with(host("alfa.localhost")).session(sa)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("desconhecida.localhost"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/configuracao").with(host("localhost")).header("X-Forwarded-Host","alfa.localhost").session(owner)).andExpect(status().isNotFound());
        mvc.perform(post("/login").with(host("localhost")).with(csrf()).param("username","admin@empresa.test").param("password","Empresa-test-12345"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/plataforma/empresas/"+a.id()+"/status").with(host("localhost")).session(owner).with(csrf())
                .contentType("application/json").content("{\"ativa\":false}")).andExpect(status().isOk());
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sa)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/configuracao").with(host("beta.localhost")).session(sb)).andExpect(status().isOk());
        mvc.perform(put("/api/plataforma/empresas/"+a.id()+"/status").with(host("localhost")).session(owner).with(csrf())
                .contentType("application/json").content("{\"ativa\":true}")).andExpect(status().isOk());
        sa.removeAttribute(TenantContext.SESSION_SCOPE);
        mvc.perform(get("/api/admin/configuracao").with(host("alfa.localhost")).session(sa)).andExpect(status().isUnauthorized());
        assertThat(TenantContext.getEmpresaId()).isNull();
        assertThatThrownBy(()->servicos.findAll()).hasRootCauseInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->servicos.saveAndFlush(new Servico())).hasRootCauseInstanceOf(IllegalStateException.class);
    }

    @Test void todosOsRepositoriosRespeitamEmpresaInclusiveFilhosAgregacoesEBuscaPorId() throws Exception {
        var owner=owner();var a=create(owner,"gama");var b=create(owner,"delta");
        var idsA=empresa(a.id(),this::operacaoCompleta);
        var idsB=empresa(b.id(),this::operacaoCompleta);
        empresa(a.id(),()->{
            var repositories=List.<org.springframework.data.jpa.repository.JpaRepository<?,Long>>of(
                    barbeiros,clientes,agendamentos,caixas,movimentos,jornadas,bloqueios,planos,contratos,recebimentos,pontos,comissoes,descontos);
            for(int i=0;i<repositories.size();i++) {
                var repo=repositories.get(i);
                assertThat(repo.count()).as(repo.toString()).isEqualTo(1);
                assertThat(repo.findAll()).hasSize(1);
                assertThat(repo.findById(idsA.get(i))).isPresent();
                assertThat(repo.findById(idsB.get(i))).isEmpty();
                assertThat(repo.existsById(idsB.get(i))).isFalse();
            }
            assertThat(horarios.count()).isEqualTo(7);
            assertThat(configuracoes.count()).isEqualTo(1);
            assertThat(usuarios.findByLogin("admin@empresa.test")).isPresent();
            assertThat(comandas.somarValoresConcluidos(LocalDate.now())).isEqualByComparingTo("10");
            assertThat(comissoes.somarComissoesConcluidasNoDia(LocalDate.now())).isEqualByComparingTo("5");
            assertThat(recebimentos.findByDataPagamentoBetweenOrderByDataPagamentoDescIdDesc(LocalDate.now(),LocalDate.now())).hasSize(1);
            return null;
        });
        var foreign=empresa(b.id(),()->servicos.findAll().getFirst());
        assertThatThrownBy(()->empresa(a.id(),()->{
            var plano=planos.findById(idsA.get(7)).orElseThrow();
            plano.setDescricao("Tentativa");plano.getServicosIncluidos().add(foreign);planos.flush();return null;
        })).isInstanceOf(RuntimeException.class);
    }

    private List<Long> operacaoCompleta() {
        var usuario=usuarios.findByLogin("admin@empresa.test").orElseThrow();
        var b=new Barbeiro();b.setNome("Barbeiro");b.setTelefone("119999999");b.setUsuario(usuario);barbeiros.saveAndFlush(b);
        var c=new Cliente();c.setNome("Cliente");c.setTelefone("119999999");c.setUsuario(usuario);clientes.saveAndFlush(c);
        var s=servico("Corte");
        var p=new PlanosMensal();p.setNomePlano("Plano");p.setDescricao("Plano");p.setValorMensal(BigDecimal.TEN);p.setAtivo(true);p.getServicosIncluidos().add(s);planos.saveAndFlush(p);
        var contrato=new PlanoMensalCliente();contrato.setCliente(c);contrato.setUsuario(usuario);contrato.setPlanoMensal(p);contrato.setValorMensal(BigDecimal.TEN);contrato.setAtivo(true);contratos.saveAndFlush(contrato);
        var a=new Agendamento();a.setCliente(c);a.setBarbeiro(b);a.setPlanoMensalCliente(contrato);a.getServico().add(s);a.setData(LocalDate.now());a.setStatus(StatusAgendamento.CONCLUIDO);a.setPagamentoRecebido(true);a.setPrecoTotal(BigDecimal.TEN);a.setFinalizadoEm(LocalDateTime.now());agendamentos.saveAndFlush(a);
        var cmd=bruninho.Barbeiro.config.ComandaTestFixtures.criar(a,BigDecimal.TEN,new BigDecimal("5"),30,comandas);
        var caixa=new Caixa();caixa.setSaldoInicial(BigDecimal.ZERO);caixa.setSaldoFinal(BigDecimal.TEN);caixa.setStatus(StatusCaixa.ABERTO);caixa.setAbertoEm(LocalDateTime.now());caixas.saveAndFlush(caixa);
        var m=new MovimentoCaixa(null,caixa,a,TipoMovimentoCaixa.ENTRADA,BigDecimal.TEN,LocalDateTime.now(),"Teste");m.setComanda(cmd);movimentos.saveAndFlush(m);
        var j=new JornadaBarbeiro();j.setBarbeiro(b);j.setDiaSemana(DayOfWeek.MONDAY);jornadas.saveAndFlush(j);
        var bl=new BloqueioBarbeiro();bl.setBarbeiro(b);bloqueios.saveAndFlush(bl);
        var r=new RecebimentoPlano();r.setContrato(contrato);r.setVencimento(LocalDate.now());r.setDataPagamento(LocalDate.now());r.setValor(BigDecimal.TEN);r.setFormaPagamento(FormaPagamento.PIX);r.setRegistradoEm(LocalDateTime.now());r.setRegistradoPor("admin");recebimentos.saveAndFlush(r);
        var cp=new ComissaoPlano();cp.setComanda(cmd);cp.setAgendamento(a);cp.setPlanoMensal(p);cp.setPlanoMensalCliente(contrato);cp.setBarbeiro(b);cp.setDataAgendamento(LocalDate.now());cp.setPontos(30);cp.setCriadoEm(LocalDateTime.now());pontos.saveAndFlush(cp);
        var cb=new ComissaoBarbeiro();cb.setComanda(cmd);cb.setAgendamento(a);cb.setBarbeiro(b);cb.setValorComissao(new BigDecimal("5"));comissoes.saveAndFlush(cb);
        var d=new ServicosPlanosDesconto();d.setPlanoMensal(p);d.setPorcentagemDesconto(10);d.getServicos().add(s);descontos.saveAndFlush(d);
        return List.of(b.getId(),c.getId(),a.getId(),caixa.getId(),m.getId(),j.getId(),bl.getId(),p.getId(),contrato.getId(),r.getId(),cp.getId(),cb.getId(),d.getId());
    }
}
