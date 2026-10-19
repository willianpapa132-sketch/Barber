package bruninho.Barbeiro.Controller.Admin.cadastros.barbeiro;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice(assignableTypes = BarbeiroAdminController.class)
public class BarbeiroAdminExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> negocio(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<Erro> validacao(Exception e) { return ResponseEntity.badRequest().body(new Erro("Confira os campos obrigatorios, e-mail, comissao e os sete dias da jornada")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Erro> conflito(DataIntegrityViolationException e) { return ResponseEntity.status(409).body(new Erro("Cadastro em conflito. Confira se o e-mail ja esta em uso e tente novamente.")); }
}
