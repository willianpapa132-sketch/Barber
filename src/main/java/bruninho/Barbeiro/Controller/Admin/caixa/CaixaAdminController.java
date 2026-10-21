package bruninho.Barbeiro.Controller.Admin.caixa;
import bruninho.Barbeiro.service.CaixaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/caixa")
public class CaixaAdminController {
 private final CaixaService service;
 public CaixaAdminController(CaixaService service){
   this.service=service;
 }
 @PostMapping @ResponseStatus(HttpStatus.CREATED)
 public CaixaResponse abrir(@Valid @RequestBody AbrirCaixaRequest r){
   return CaixaResponse.de(service.abrir(r));
 }
 @GetMapping public CaixaResponse atual(){
   return CaixaResponse.de(service.atual());
 }
 @GetMapping("/historico")
 public java.util.List<CaixaResponse> listar(){
   return service.listar().stream().map(CaixaResponse::de).toList();
 }
 @GetMapping("/{id}/movimentacoes")
 public java.util.List<MovimentoCaixaResponse> movimentos(@PathVariable Long id){
   return service.movimentos(id);
 }
 @PostMapping("/sangrias")
 public CaixaResponse sangria(@Valid @RequestBody SangriaRequest r){
   return CaixaResponse.de(service.sangria(r));
 }
 @PostMapping("/fechamento")
 public CaixaResponse fechar(){
   return CaixaResponse.de(service.fechar());
 }
}
