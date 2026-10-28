package bruninho.Barbeiro.platform;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=PlatformController.class)
public class PlatformExceptionHandler {
    public record Error(String mensagem){}
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<Error> status(ResponseStatusException e){return ResponseEntity.status(e.getStatusCode()).body(new Error(e.getReason()));}
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class})
    public ResponseEntity<Error> invalid(Exception e){return ResponseEntity.badRequest().body(new Error("Confira nome, responsavel, endereco, e-mail e senha inicial da empresa"));}
}
