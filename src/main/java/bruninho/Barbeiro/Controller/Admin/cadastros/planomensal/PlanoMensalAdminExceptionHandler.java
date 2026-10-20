package bruninho.Barbeiro.Controller.Admin.cadastros.planomensal;

import bruninho.Barbeiro.exception.BusinessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = PlanoMensalAdminController.class)
public class PlanoMensalAdminExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<PlanoMensalAdminErro> negocio(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new PlanoMensalAdminErro(e.getReason()));
    }
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<PlanoMensalAdminErro> regra(BusinessException e) {
        return ResponseEntity.badRequest().body(new PlanoMensalAdminErro(e.getMessage()));
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<PlanoMensalAdminErro> validacao(Exception e) {
        return ResponseEntity.badRequest().body(new PlanoMensalAdminErro("Confira nome, descrição, valor, antecedência, serviços e descontos do plano."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<PlanoMensalAdminErro> conflito(DataIntegrityViolationException e) {
        return ResponseEntity.status(409).body(new PlanoMensalAdminErro("O plano possui vínculos ou dados em conflito. Atualize a tela e tente novamente."));
    }
}
