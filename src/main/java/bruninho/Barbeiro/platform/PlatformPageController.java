package bruninho.Barbeiro.platform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller @ConditionalOnProperty(name="app.multi-tenancy.enabled",havingValue="true")
public class PlatformPageController {
    @GetMapping("/plataforma") public String page(){return "forward:/client/index.html";}
}
