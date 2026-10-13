package bruninho.Barbeiro.Controller.cliente.planos;

import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = PlanoClienteController.class)
public class PlanoClienteExceptionHandler {
    public record ErroPlano(String mensagem) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroPlano> naoEncontrado(NotFoundException exception) {
        return ResponseEntity.status(404).body(new ErroPlano(exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroPlano> regra(BusinessException exception) {
        return ResponseEntity.badRequest().body(new ErroPlano(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroPlano> entrada(Exception exception) {
        return ResponseEntity.badRequest().body(new ErroPlano("Dados invalidos para o plano."));
    }
}
