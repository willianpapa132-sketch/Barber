package bruninho.Barbeiro.Controller.cliente.perfil;

import bruninho.Barbeiro.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = ClientePerfilController.class)
public class ClientePerfilExceptionHandler {
    public record ErroPerfil(String mensagem) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroPerfil> naoEncontrado(NotFoundException exception) {
        return ResponseEntity.status(404).body(new ErroPerfil(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroPerfil> entrada() {
        return ResponseEntity.badRequest().body(new ErroPerfil("Informe nome, telefone e imagem validos."));
    }
}
