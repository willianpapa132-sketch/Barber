package bruninho.Barbeiro.Controller.Admin.caixa;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = CaixaAdminController.class)
public class CaixaAdminExceptionHandler {
    public record Erro(String mensagem) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> negocio(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Erro> invalido(Exception e) {
        return ResponseEntity.badRequest().body(new Erro("Informe um valor válido para a operação."));
    }
}
