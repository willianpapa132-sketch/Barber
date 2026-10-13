package bruninho.Barbeiro.Controller.cliente.agendamentos;

import bruninho.Barbeiro.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = AgendamentosClienteController.class)
public class AgendamentosClienteExceptionHandler {
    public record ErroAgendamento(String mensagem) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroAgendamento> naoEncontrado(NotFoundException exception) {
        return ResponseEntity.status(404).body(new ErroAgendamento(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroAgendamento> entrada() {
        return ResponseEntity.badRequest().body(new ErroAgendamento("Agendamento invalido."));
    }
}
