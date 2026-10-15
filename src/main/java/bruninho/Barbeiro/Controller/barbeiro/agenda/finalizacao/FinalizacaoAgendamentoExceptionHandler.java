package bruninho.Barbeiro.Controller.barbeiro.agenda.finalizacao;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = FinalizacaoAgendamentoController.class)
public class FinalizacaoAgendamentoExceptionHandler {
    public record ErroFinalizacao(String mensagem) {}

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroFinalizacao> regra(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(new ErroFinalizacao(exception.getReason()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroFinalizacao> entrada(Exception exception) {
        return ResponseEntity.badRequest().body(new ErroFinalizacao("Informe um agendamento e uma forma de pagamento validos"));
    }
}
