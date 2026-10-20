package bruninho.Barbeiro.Controller.Admin.cadastros.servicos;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=ServicoAdminController.class)
public class ServicoAdminExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> negocio(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason())); }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class, org.springframework.http.converter.HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Erro> invalido(Exception e) { return ResponseEntity.badRequest().body(new Erro("Confira nome, preço, duração e status do serviço.")); }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Erro> vinculo(Exception e) { return ResponseEntity.status(409).body(new Erro("Não foi possível concluir: o serviço possui vínculos ou dados inválidos.")); }
}
