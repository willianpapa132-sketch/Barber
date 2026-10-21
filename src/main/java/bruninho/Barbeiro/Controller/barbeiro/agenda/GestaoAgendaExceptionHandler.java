package bruninho.Barbeiro.Controller.barbeiro.agenda;

import bruninho.Barbeiro.exception.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {GestaoAgendaBarbeiroController.class,
        bruninho.Barbeiro.Controller.Admin.agendamentos.AgendamentosAdminController.class})
public class GestaoAgendaExceptionHandler {
    public record Erro(String mensagem) {}
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Erro> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new Erro(e.getReason()));
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Erro> naoEncontrado(NotFoundException e) { return ResponseEntity.status(404).body(new Erro(e.getMessage())); }
    @ExceptionHandler(HorarioIndisponivelException.class)
    public ResponseEntity<Erro> conflito(HorarioIndisponivelException e) { return ResponseEntity.status(409).body(new Erro(e.getMessage())); }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Erro> regra(BusinessException e) { return ResponseEntity.badRequest().body(new Erro(e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Erro> entrada(Exception e) { return ResponseEntity.badRequest().body(new Erro("Informe cliente, servicos, data e horarios validos")); }
}
