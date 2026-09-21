package promo67.login.invent.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Este método interceptará cualquier IllegalArgumentException que lance tu verificador de Google
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidToken(IllegalArgumentException ex) {
        
        System.out.println("--- Acceso denegado ---");
        System.out.println("Motivo: " + ex.getMessage());
        
        // Retornamos un 401 en lugar del 500 por defecto
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token de autenticación inválido, mal formado o expirado.");
    }
}