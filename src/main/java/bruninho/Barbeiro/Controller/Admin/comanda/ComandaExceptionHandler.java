package bruninho.Barbeiro.Controller.Admin.comanda;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=ComandaAdminController.class)
public class ComandaExceptionHandler {
    public record Erro(String mensagem){}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Erro> erro(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));}
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Erro> dados(Exception e){return ResponseEntity.badRequest().body(new Erro("Confira itens, quantidades, desconto e pagamentos da comanda"));}
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.dao.ConcurrencyFailureException.class})
    public ResponseEntity<Erro> conflito(Exception e){return ResponseEntity.status(409).body(new Erro("A comanda ou o estoque mudou. Atualize os dados antes de tentar novamente"));}
}
