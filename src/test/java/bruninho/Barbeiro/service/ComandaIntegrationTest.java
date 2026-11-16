package bruninho.Barbeiro.service;
import bruninho.Barbeiro.Controller.Admin.comanda.*;
import bruninho.Barbeiro.Controller.Admin.comanda.FecharComandaRequest.*;
import bruninho.Barbeiro.domain.*;
import bruninho.Barbeiro.repository.*;
import bruninho.Barbeiro.security.model.*;
import bruninho.Barbeiro.security.repository.UsuarioRepository;
import bruninho.Barbeiro.platform.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:comandas_test;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH")
@ActiveProfiles("test") @AutoConfigureMockMvc @WithMockUser(username="admin",roles="ADMIN")
class ComandaIntegrationTest {
    @org.springframework.test.context.DynamicPropertySource
    static void postgres(org.springframework.test.context.DynamicPropertyRegistry r){
        if(System.getenv("COMANDA_TEST_DB_URL")==null)return;
        r.add("spring.datasource.url",()->System.getenv("COMANDA_TEST_DB_URL"));r.add("spring.datasource.username",()->"tenant_test");r.add("spring.datasource.password",()->"");
        r.add("spring.datasource.driver-class-name",()->"org.postgresql.Driver");r.add("spring.jpa.properties.hibernate.dialect",()->"org.hibernate.dialect.PostgreSQLDialect");
        r.add("spring.jpa.hibernate.ddl-auto",()->"validate");r.add("spring.flyway.enabled",()->"true");
    }
    @Autowired MockMvc mvc; @Autowired ComandaService service; @Autowired TransactionTemplate tx;
    @Autowired AgendamentoRepository agendamentos; @Autowired BarbeiroRepository barbeiros; @Autowired UsuarioRepository usuarios;
    @Autowired ServicoRepository servicos; @Autowired ProdutoRepository produtos; @Autowired ComandaRepository comandas;
    @Autowired CaixaRepository caixas; @Autowired MovimentoCaixaRepository movimentos; @Autowired ComissaoBarbeiroRepository comissoes;
    @Autowired ComissaoPlanoRepository pontos; @Autowired PlanoMensalClienteRepository contratos; @Autowired PlanoMensalRepository planos;
    @Autowired ClienteRepository clientes; @Autowired TenantRegistry empresas;
    private final JsonMapper json=new JsonMapper();
    private Long a,s,p,b;
    @BeforeEach void preparar(){tx.executeWithoutResult(status->{
        movimentos.deleteAll();comissoes.deleteAll();pontos.deleteAll();comandas.deleteAll();agendamentos.deleteAll();contratos.deleteAll();planos.deleteAll();clientes.deleteAll();produtos.deleteAll();servicos.deleteAll();barbeiros.deleteAll();usuarios.deleteAll();caixas.deleteAll();usuarios.flush();
        var u=new Usuario();u.setLogin("barbeiro");u.setSenha("hash");u.setRole(ROLE.BARBEIRO);u.setAtivo(true);usuarios.save(u);
        var br=new Barbeiro();br.setNome("Bruno");br.setTelefone("119999999");br.setUsuario(u);br.setPorcentagemComissao(50);b=barbeiros.save(br).getId();
        var se=new Servico();se.setNome("Corte");se.setPreco(new BigDecimal("100"));se.setDuracaoMinutos(30);s=servicos.save(se).getId();
        var pr=new Produto();pr.setNome("Pomada");pr.setPreco(new BigDecimal("50"));pr.setPercentualComissao(new BigDecimal("10"));pr.setPeriodicidadeMeses(1);pr.setQuantidadeEstoque(5);p=produtos.save(pr).getId();
        var ag=new Agendamento();ag.setBarbeiro(br);ag.setNomeCliente("Carlos");ag.setData(LocalDate.now().minusDays(4));ag.setHoraInicio(LocalTime.of(10,0));ag.setHoraFinalizacao(LocalTime.of(10,30));ag.setStatus(StatusAgendamento.AGENDADO);ag.setServico(new HashSet<>(List.of(se)));ag.setPrecoTotal(new BigDecimal("999"));a=agendamentos.save(ag).getId();
    });}
    private FecharComandaRequest request(){return new FecharComandaRequest(List.of(new Item(TipoItem.SERVICO,s,1),new Item(TipoItem.PRODUTO,p,2)),TipoDesconto.PERCENTUAL,new BigDecimal("10"),"Fidelidade",List.of(new Pagamento(FormaPagamento.PIX,new BigDecimal("80")),new Pagamento(FormaPagamento.CREDITO,new BigDecimal("100"))));}
    private void abrir()throws Exception {mvc.perform(post("/api/admin/caixa").with(user("admin").roles("ADMIN")).contentType("application/json").content("{\"saldoInicial\":0}")).andExpect(status().isCreated());}
    private org.springframework.test.web.servlet.ResultActions fechar(FecharComandaRequest r)throws Exception{return mvc.perform(post("/api/admin/comandas/agendamento/"+a+"/fechar").with(user("admin").roles("ADMIN")).contentType("application/json").content(json.writeValueAsString(r)));}
    @Test void fechaComandaComDescontoRateadoEstoquePagamentosEComissoes()throws Exception{
        abrir();fechar(request()).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(180)).andExpect(jsonPath("$.subtotal").value(200)).andExpect(jsonPath("$.desconto").value(20)).andExpect(jsonPath("$.totalComissao").value(54)).andExpect(jsonPath("$.pagamentos.length()").value(2));
        assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(3);
        assertThat(caixas.findAll().getFirst().getTotalEntradas()).isEqualByComparingTo("180");
        assertThat(movimentos.count()).isEqualTo(1);assertThat(comissoes.findAll().getFirst().getComanda()).isNotNull();
        assertThat(comissoes.findAll().getFirst().getValorComissao()).isEqualByComparingTo("54");
        fechar(request()).andExpect(status().isConflict());assertThat(movimentos.count()).isEqualTo(1);
    }
    @Test void valoresFinanceirosUsamFechamentoENaoAgendamentoOuCatalogo()throws Exception{
        abrir();fechar(request()).andExpect(status().isOk());
        tx.executeWithoutResult(status->{var ag=agendamentos.findById(a).orElseThrow();ag.setPrecoTotal(new BigDecimal("9000"));ag.setFinalizadoEm(LocalDateTime.now().minusYears(1));var pr=produtos.findById(p).orElseThrow();pr.setNome("Nome alterado");pr.setPreco(new BigDecimal("999"));pr.setPercentualComissao(new BigDecimal("99"));});
        var query="?inicio="+LocalDate.now()+"&fim="+LocalDate.now();
        mvc.perform(get("/api/admin/financeiro"+query).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.valorBruto").value(180)).andExpect(jsonPath("$.despesas").value(54)).andExpect(jsonPath("$.entradas.length()").value(2));
        mvc.perform(get("/api/admin/deshboard").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.entradasComandas").value(180)).andExpect(jsonPath("$.despesas").value(54)).andExpect(jsonPath("$.concluidos").value(1));
        mvc.perform(get("/api/admin/home").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.valorLiquidoHoje").value(126));
        mvc.perform(get("/api/barbeiro/carteira"+query).with(user("barbeiro").roles("BARBEIRO"))).andExpect(status().isOk()).andExpect(jsonPath("$.valorLiquido").value(54));
        mvc.perform(get("/api/admin/comandas/agendamento/"+a).with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(jsonPath("$.itens[1].nome").value("Pomada")).andExpect(jsonPath("$.totalComissao").value(54));
    }
    @Test void falhaDeCaixaDesfazComandaEstoqueComissaoEStatus()throws Exception{
        fechar(request()).andExpect(status().isConflict());assertThat(comandas.count()).isZero();assertThat(comissoes.count()).isZero();assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(5);assertThat(agendamentos.findById(a).orElseThrow().getStatus()).isEqualTo(StatusAgendamento.AGENDADO);
    }
    @Test void rejeitaPagamentoDivergenteDescontoInvalidoEEstoqueInsuficiente()throws Exception{
        abrir();var r=request();
        fechar(new FecharComandaRequest(r.itens(),r.tipoDesconto(),r.desconto(),null,List.of(new Pagamento(FormaPagamento.PIX,new BigDecimal("179.99"))))).andExpect(status().isConflict());
        fechar(new FecharComandaRequest(r.itens(),TipoDesconto.VALOR,new BigDecimal("201"),null,r.pagamentos())).andExpect(status().isBadRequest());
        fechar(new FecharComandaRequest(List.of(new Item(TipoItem.PRODUTO,p,6)),TipoDesconto.NENHUM,BigDecimal.ZERO,null,List.of(new Pagamento(FormaPagamento.PIX,new BigDecimal("300"))))).andExpect(status().isConflict());
        fechar(new FecharComandaRequest(r.itens(),r.tipoDesconto(),r.desconto(),null,List.of(new Pagamento(FormaPagamento.MENSAL,new BigDecimal("180"))))).andExpect(status().isBadRequest());
        assertThat(comandas.count()).isZero();assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(5);
    }
    @Test void somenteAdminPodeFecharENaoExisteMaisFinalizacaoDoBarbeiro()throws Exception{
        mvc.perform(post("/api/admin/comandas/agendamento/"+a+"/fechar").with(user("barbeiro").roles("BARBEIRO")).contentType("application/json").content(json.writeValueAsString(request()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/barbeiro/agendamentos/"+a+"/finalizacao").with(user("barbeiro").roles("BARBEIRO")).contentType("application/json").content("{\"formaPagamento\":\"PIX\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/barbeiro/agendamentos/"+a+"/finalizacao").with(user("barbeiro").roles("BARBEIRO"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/agendamentos/"+a+"/comanda").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andExpect(forwardedUrl("/client/index.html"));
    }
    @Test void rejeitaProdutoDeOutraEmpresa()throws Exception{
        var id=UUID.randomUUID();empresas.central().update("insert into platform_empresa(id,nome,responsavel,slug,estado,criado_em) values (?,'Outra','Dono',?,'ATIVA',CURRENT_TIMESTAMP)",id,"outra-"+id.toString().replace("-",""));
        var anterior=TenantContext.requireEmpresaId();Long externo;
        try{TenantContext.setEmpresaId(id);externo=tx.execute(status->{var pr=new Produto();pr.setNome("Externo");pr.setPreco(BigDecimal.TEN);pr.setQuantidadeEstoque(10);pr.setPeriodicidadeMeses(1);return produtos.save(pr).getId();});}finally{TenantContext.setEmpresaId(anterior);}
        abrir();fechar(new FecharComandaRequest(List.of(new Item(TipoItem.PRODUTO,externo,1)),TipoDesconto.NENHUM,BigDecimal.ZERO,null,List.of(new Pagamento(FormaPagamento.PIX,BigDecimal.TEN)))).andExpect(status().isNotFound());
    }
    @Test void descontoTotalNaoGeraReceitaOuComissaoMasBaixaEstoque()throws Exception{
        fechar(new FecharComandaRequest(List.of(new Item(TipoItem.PRODUTO,p,1)),TipoDesconto.PERCENTUAL,new BigDecimal("100"),null,List.of())).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(4);assertThat(movimentos.count()).isZero();assertThat(comissoes.count()).isZero();
    }
    @Test void planoIncluidoFechaSemReceitaEGeraPontosPelaComanda()throws Exception{
        tx.executeWithoutResult(status->{var u=new Usuario();u.setLogin("cliente");u.setSenha("hash");u.setRole(ROLE.CLIENTE);u.setAtivo(true);usuarios.save(u);var cliente=new Cliente();cliente.setUsuario(u);cliente.setNome("Cliente Plano");cliente.setTelefone("119999999");clientes.save(cliente);
            var pl=new PlanosMensal();pl.setNomePlano("Mensal");pl.setDescricao("Plano");pl.setValorMensal(BigDecimal.TEN);pl.setAtivo(true);pl.setServicosIncluidos(new HashSet<>(List.of(servicos.findById(s).orElseThrow())));planos.save(pl);
            var ct=new PlanoMensalCliente();ct.setCliente(cliente);ct.setUsuario(u);ct.setPlanoMensal(pl);ct.setValorMensal(BigDecimal.TEN);ct.setAtivo(true);ct.setProximoVencimento(LocalDate.now().plusMonths(1));contratos.save(ct);var ag=agendamentos.findById(a).orElseThrow();ag.setCliente(cliente);ag.setPlanoMensalCliente(ct);
        });
        fechar(new FecharComandaRequest(List.of(new Item(TipoItem.SERVICO,s,1)),TipoDesconto.NENHUM,BigDecimal.ZERO,null,List.of())).andExpect(status().isOk()).andExpect(jsonPath("$.pontos").value(30)).andExpect(jsonPath("$.total").value(0));
        assertThat(pontos.count()).isEqualTo(1);assertThat(pontos.findAll().getFirst().getComanda()).isNotNull();assertThat(movimentos.count()).isZero();
    }
    @Test void rateioDeCentavosFechaExatamenteComTresItens()throws Exception{
        abrir();Long extra=tx.execute(status->{servicos.findById(s).orElseThrow().setPreco(new BigDecimal("0.01"));produtos.findById(p).orElseThrow().setPreco(new BigDecimal("0.01"));var se=new Servico();se.setNome("Extra");se.setPreco(new BigDecimal("0.01"));se.setDuracaoMinutos(10);return servicos.save(se).getId();});
        fechar(new FecharComandaRequest(List.of(new Item(TipoItem.SERVICO,s,1),new Item(TipoItem.PRODUTO,p,1),new Item(TipoItem.SERVICO,extra,1)),TipoDesconto.VALOR,new BigDecimal("0.01"),null,List.of(new Pagamento(FormaPagamento.PIX,new BigDecimal("0.02")))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0.02)).andExpect(jsonPath("$.desconto").value(0.01)).andExpect(jsonPath("$.itens[0].desconto").value(0.01)).andExpect(jsonPath("$.totalComissao").value(0.01));
    }
    @Test void comandasDiferentesDisputandoMesmoProdutoNaoVendemAlemDoEstoque()throws Exception{
        abrir();var empresa=TenantContext.requireEmpresaId();Long segundo=tx.execute(status->{var ag=new Agendamento();ag.setBarbeiro(barbeiros.findById(b).orElseThrow());ag.setNomeCliente("Outro");ag.setData(LocalDate.now().minusDays(1));ag.setHoraInicio(LocalTime.of(10,0));ag.setStatus(StatusAgendamento.AGENDADO);return agendamentos.save(ag).getId();});
        var r=new FecharComandaRequest(List.of(new Item(TipoItem.PRODUTO,p,4)),TipoDesconto.NENHUM,BigDecimal.ZERO,null,List.of(new Pagamento(FormaPagamento.PIX,new BigDecimal("200"))));
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            var futures=new ArrayList<Future<Integer>>();
            for(var id:List.of(a,segundo))futures.add(executor.submit(()->{
                TenantContext.setEmpresaId(empresa);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
                try{start.await(5,TimeUnit.SECONDS);service.fechar(id,r,"admin");return 200;}catch(org.springframework.web.server.ResponseStatusException e){return e.getStatusCode().value();}finally{TenantContext.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}
            }));
            start.countDown();assertThat(List.of(futures.getFirst().get(20,TimeUnit.SECONDS),futures.getLast().get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(1);assertThat(comandas.count()).isEqualTo(1);assertThat(caixas.findAll().getFirst().getTotalEntradas()).isEqualByComparingTo("200");
    }
    @Test void fechamentoConcorrenteNaoDuplicaComandaOuEstoque()throws Exception{
        abrir();var empresa=TenantContext.requireEmpresaId();var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)){
            Callable<Integer> fechar=()->{TenantContext.setEmpresaId(empresa);org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("admin","",List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
                try{start.await(5,TimeUnit.SECONDS);service.fechar(a,request(),"admin");return 200;}catch(org.springframework.web.server.ResponseStatusException e){return e.getStatusCode().value();}finally{TenantContext.clear();org.springframework.security.core.context.SecurityContextHolder.clearContext();}};
            var f1=executor.submit(fechar);var f2=executor.submit(fechar);start.countDown();assertThat(List.of(f1.get(20,TimeUnit.SECONDS),f2.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(200,409);
        }
        assertThat(comandas.count()).isEqualTo(1);assertThat(movimentos.count()).isEqualTo(1);assertThat(produtos.findById(p).orElseThrow().getQuantidadeEstoque()).isEqualTo(3);
    }
}
