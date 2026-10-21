package bruninho.Barbeiro.Controller.Admin.deshboard;
import bruninho.Barbeiro.service.DashboardAdminService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
@RestController
@RequestMapping("/api/admin/deshboard")
public class DashboardAdminController {
    private final DashboardAdminService service;
    public DashboardAdminController(DashboardAdminService service){this.service=service;}
    @GetMapping
    public DashboardResponse consultar(@RequestParam(defaultValue="hoje") String periodo,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate referencia,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate fim) {
        return service.consultar(periodo,referencia,inicio,fim);
    }
}
