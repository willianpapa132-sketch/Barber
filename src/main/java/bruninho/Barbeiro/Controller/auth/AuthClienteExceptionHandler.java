package bruninho.Barbeiro.Controller.auth;

import bruninho.Barbeiro.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = AuthClienteController.class)
public class AuthClienteExceptionHandler {
    public record ErroCadastro(String mensagem) {}

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroCadastro> regra(BusinessException exception) {
        return ResponseEntity.badRequest().body(new ErroCadastro(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroCadastro> entrada() {
        return ResponseEntity.badRequest().body(new ErroCadastro("Informe nome, telefone, e-mail e senha validos."));
    }
}
