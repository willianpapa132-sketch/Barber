package bruninho.Barbeiro.platform;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import javax.sql.DataSource;
import java.util.Map;

public final class TenantRoutingDataSource extends AbstractRoutingDataSource {
    private final TenantRegistry registry;
    private final DataSource bootstrap;
    private volatile boolean initializing=true;
    public TenantRoutingDataSource(TenantRegistry registry,DataSource bootstrap){
        this.registry=registry;this.bootstrap=bootstrap;
        setTargetDataSources(Map.of());setDefaultTargetDataSource(bootstrap);setLenientFallback(false);afterPropertiesSet();
    }
    @Override protected Object determineCurrentLookupKey(){return TenantContext.get();}
    @Override protected DataSource determineTargetDataSource(){
        String scope=TenantContext.get();
        if(scope==null && initializing)return bootstrap;
        if(scope==null || TenantContext.PLATFORM.equals(scope))throw new IllegalStateException("Operacao da barbearia sem empresa autenticada");
        var company=registry.byId(java.util.UUID.fromString(scope));
        if(!"ATIVA".equals(company.estado()))throw new IllegalStateException("Empresa indisponivel");
        return registry.dataSource(company);
    }
    public void initialized(){initializing=false;}
}
