package bruninho.Barbeiro.Controller.cliente;

import bruninho.Barbeiro.exception.BusinessException;
import bruninho.Barbeiro.exception.NotFoundException;
import bruninho.Barbeiro.exception.HorarioIndisponivelException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(assignableTypes = AgendamentoClienteController.class)
public class AgendamentoExceptionHandler {
    public record ErroAgendamento(String mensagem) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroAgendamento> naoEncontrado(NotFoundException exception) {
        return ResponseEntity.status(404).body(new ErroAgendamento(exception.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroAgendamento> regra(BusinessException exception) {
        return ResponseEntity.badRequest().body(new ErroAgendamento(exception.getMessage()));
    }

    @ExceptionHandler(HorarioIndisponivelException.class)
    public ResponseEntity<ErroAgendamento> conflito(HorarioIndisponivelException exception) {
        return ResponseEntity.status(409).body(new ErroAgendamento(exception.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroAgendamento> entrada(Exception exception) {
        return ResponseEntity.badRequest().body(new ErroAgendamento(
                "Dados inválidos: informe IDs positivos, serviços, data e horário válidos"));
    }
}
