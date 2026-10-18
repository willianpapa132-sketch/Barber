package bruninho.Barbeiro.Controller.barbeiro.carteira;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {CarteiraBarbeiroController.class,
        bruninho.Barbeiro.Controller.barbeiro.carteira.valoresFormulado.ValoresFormuladoController.class})
public class CarteiraBarbeiroExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> erro(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(Map.of("mensagem", exception.getReason() == null ? "Nao foi possivel consultar a carteira" : exception.getReason()));
    }
}
