package bruninho.Barbeiro.config;

import bruninho.Barbeiro.platform.TenantContext;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import java.util.UUID;

/** O tenant dos testes antigos e explicito e existe somente no classpath de teste. */
public class EmpresaTestExecutionListener extends AbstractTestExecutionListener {
    public static final UUID EMPRESA = UUID.fromString("00000000-0000-0000-0000-000000000001");
    @Override public int getOrder() { return 3500; }
    @Override public void beforeTestMethod(TestContext context) {
        var props = context.getApplicationContext().getBean(AppProperties.class);
        if (!props.getMultiTenancy().isEnabled()) TenantContext.setEmpresaId(EMPRESA);
    }
    @Override public void afterTestMethod(TestContext context) { TenantContext.clear(); }
}
