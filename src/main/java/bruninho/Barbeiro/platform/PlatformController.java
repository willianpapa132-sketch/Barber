package bruninho.Barbeiro.platform;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/plataforma")
@ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true")
public class PlatformController {

    private final PlatformCompanyService service;

    public PlatformController(PlatformCompanyService s){
        service=s;
    }
    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token){
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/empresas")
    public List<PlatformCompanyService.Company> list(){
        return service.list();
    }
    @PostMapping("/empresas")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformCompanyService.Company create(@Valid @RequestBody PlatformCompanyService.Create r){
        return service.create(r);
    }
    @PutMapping("/empresas/{id}/status")
    public PlatformCompanyService.Company status(@PathVariable UUID id,@Valid @RequestBody PlatformCompanyService.Status r){
        return service.status(id,r.ativa());
    }
}
