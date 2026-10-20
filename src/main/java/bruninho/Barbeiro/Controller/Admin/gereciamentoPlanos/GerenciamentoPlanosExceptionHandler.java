package bruninho.Barbeiro.Controller.Admin.gereciamentoPlanos;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes = GerenciamentoPlanosController.class)
public class GerenciamentoPlanosExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> negocio(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason())); }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<Erro> invalido(Exception e) { return ResponseEntity.badRequest().body(new Erro("Confira os dados informados.")); }
}
