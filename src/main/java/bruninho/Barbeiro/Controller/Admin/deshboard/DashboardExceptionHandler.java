package bruninho.Barbeiro.Controller.Admin.deshboard;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=DashboardAdminController.class)
public class DashboardExceptionHandler {
    public record Erro(String mensagem){}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));}
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Erro> invalido(Exception e){return ResponseEntity.badRequest().body(new Erro("Informe datas validas"));}
}
