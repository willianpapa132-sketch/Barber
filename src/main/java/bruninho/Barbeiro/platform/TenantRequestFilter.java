package bruninho.Barbeiro.platform;

import bruninho.Barbeiro.config.AppProperties;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Locale;

@Component
@ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true")
public class TenantRequestFilter extends OncePerRequestFilter {
    private final TenantRegistry registry;private final AppProperties props;
    public TenantRequestFilter(TenantRegistry registry,AppProperties props){this.registry=registry;this.props=props;}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws ServletException,IOException {
        TenantContext.clear();
        try{
            var p=props.getMultiTenancy();String host=request.getServerName().toLowerCase(Locale.ROOT);String scope;
            boolean platform=host.equals(p.getPlatformHost().toLowerCase(Locale.ROOT));
            if(platform)scope=TenantContext.PLATFORM;
            else{
                String suffix="."+p.getBaseDomain().toLowerCase(Locale.ROOT);
                if(!host.endsWith(suffix)){reject(response,404,"Empresa nao encontrada");return;}
                String slug=host.substring(0,host.length()-suffix.length());
                if(!slug.matches("[a-z][a-z0-9-]{2,39}")){reject(response,404,"Empresa nao encontrada");return;}
                var c=registry.find(slug);
                if(c.isEmpty()){reject(response,404,"Empresa nao encontrada");return;}
                if(!"ATIVA".equals(c.get().estado())){reject(response,403,"Empresa indisponivel");return;}
                scope=c.get().id().toString();
            }
            TenantContext.set(scope);
            var session=request.getSession(false);
            if(session!=null){
                Object bound=session.getAttribute(TenantContext.SESSION_SCOPE);
                if(bound!=null&&!scope.equals(bound)){reject(response,403,"Esta sessao pertence a outra empresa");return;}
                if(bound==null&&session.getAttribute("SPRING_SECURITY_CONTEXT")!=null){reject(response,401,"Entre novamente neste endereco");return;}
            }
            String path=request.getRequestURI().substring(request.getContextPath().length());
            if(platform){
                if(path.equals("/")){response.sendRedirect("/plataforma");return;}
                boolean allowed=path.equals("/login")||path.equals("/logout")||path.equals("/plataforma")||path.startsWith("/api/plataforma/")
                        ||path.startsWith("/client/")||path.startsWith("/css/")||path.startsWith("/js/")||path.startsWith("/images/")||path.equals("/favicon.ico")||path.equals("/error");
                if(!allowed){reject(response,404,"Acesse o endereco da sua empresa");return;}
            }else if(path.equals("/plataforma")||path.startsWith("/api/plataforma/")){reject(response,403,"Acesso exclusivo da plataforma");return;}
            chain.doFilter(request,response);
        }finally{TenantContext.clear();}
    }
    private void reject(HttpServletResponse r,int status,String message)throws IOException{
        r.setStatus(status);r.setContentType("application/json;charset=UTF-8");r.getWriter().write("{\"mensagem\":\""+message+"\"}");
    }
}
