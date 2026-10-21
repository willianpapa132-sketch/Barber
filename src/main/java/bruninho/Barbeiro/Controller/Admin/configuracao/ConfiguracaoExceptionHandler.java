package bruninho.Barbeiro.Controller.Admin.configuracao;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=ConfiguracaoController.class)
public class ConfiguracaoExceptionHandler {
    public record Erro(String mensagem){}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<Erro> invalido(Exception e){return ResponseEntity.badRequest().body(new Erro("Confira nome, antecedencia de 0 a 30 dias e os sete horarios de funcionamento."));}
}
