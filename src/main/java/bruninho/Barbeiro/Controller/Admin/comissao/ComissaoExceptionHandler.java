package bruninho.Barbeiro.Controller.Admin.comissao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes = ComissaoAdminController.class)
public class ComissaoExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erro> invalido(Exception e) {
        return ResponseEntity.badRequest().body(new Erro("Informe datas e barbeiro validos"));
    }
}
