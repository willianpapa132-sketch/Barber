package bruninho.Barbeiro.config;

import bruninho.Barbeiro.platform.*;
import bruninho.Barbeiro.service.GerenciamentoPlanosService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TenantLifecycleTest {
    @Test void filtroLimpaContextoMesmoQuandoAplicacaoFalha() {
        var props=new AppProperties();var registry=mock(TenantRegistry.class);var id=UUID.randomUUID();
        when(registry.find("alfa")).thenReturn(Optional.of(new TenantRegistry.Company(id,"Alfa","Dono","alfa","ATIVA")));
        var filter=new TenantRequestFilter(registry,props);
        var request=new MockHttpServletRequest("GET","/api/admin/configuracao");request.setServerName("alfa.localhost");
        assertThatThrownBy(()->filter.doFilter(request,new MockHttpServletResponse(),(req,res)->{
            assertThat(TenantContext.requireEmpresaId()).isEqualTo(id);
            throw new jakarta.servlet.ServletException("Falha da aplicacao");
        })).isInstanceOf(jakarta.servlet.ServletException.class);
        assertThat(TenantContext.getEmpresaId()).isNull();assertThat(TenantContext.isPlatform()).isFalse();
    }
    @Test void contextosConcorrentesNaoSeMisturam() throws Exception {
        var ids=List.of(UUID.randomUUID(),UUID.randomUUID());var barreira=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var futures=new ArrayList<Future<UUID>>();
            for(var id:ids)futures.add(executor.submit(()->{
                try { TenantContext.setEmpresaId(id);barreira.await(5,TimeUnit.SECONDS);return TenantContext.requireEmpresaId(); }
                finally { TenantContext.clear(); }
            }));
            assertThat(futures.getFirst().get(10,TimeUnit.SECONDS)).isEqualTo(ids.getFirst());
            assertThat(futures.getLast().get(10,TimeUnit.SECONDS)).isEqualTo(ids.getLast());
        }
        assertThat(TenantContext.getEmpresaId()).isNull();
    }
    @Test void agendadorProcessaApenasAtivasEContinuaAposFalhaSemVazarContexto() {
        var registry=mock(TenantRegistry.class);var service=mock(GerenciamentoPlanosService.class);
        var a=UUID.randomUUID();var b=UUID.randomUUID();var suspensa=UUID.randomUUID();
        when(registry.all()).thenReturn(List.of(new TenantRegistry.Company(a,"A","Dono","aaa","ATIVA"),
                new TenantRegistry.Company(suspensa,"S","Dono","sss","SUSPENSA"),new TenantRegistry.Company(b,"B","Dono","bbb","ATIVA")));
        var processadas=new ArrayList<UUID>();
        doAnswer(invocation->{
            var id=TenantContext.requireEmpresaId();processadas.add(id);
            if(id.equals(a))throw new IllegalStateException("Falha simulada");return null;
        }).when(service).atualizarBloqueios();
        new BloqueioPlanosScheduler(service,registry).atualizar();
        assertThat(processadas).containsExactly(a,b);assertThat(TenantContext.getEmpresaId()).isNull();
    }
}
