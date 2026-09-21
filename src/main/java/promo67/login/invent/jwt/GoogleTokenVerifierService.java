package promo67.login.invent.jwt;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

@Service
public class GoogleTokenVerifierService {

    // Se inyecta desde application.properties
    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    /**
    * Valida el token de Google y retorna el Payload si es válido.
    * Si es inválido, ha expirado o pertenece a otra app, retorna null.
    */
    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                // 1. Verifica que el campo 'aud' coincida con tu Client ID
                .setAudience(Collections.singletonList(clientId))
                // 2 & 3. La firma criptográfica y la expiración se validan automáticamente al hacer build y verify
                .build();

        try {
            // Este método internamente descarga las llaves públicas de Google para verificar la firma
            GoogleIdToken idToken = verifier.verify(idTokenString);
            
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                
                // Aquí ya tienes acceso seguro a la identidad de Google
                // String email = payload.getEmail();
                // String name = (String) payload.get("name");
                // String pictureUrl = (String) payload.get("picture");
                
                return payload;
            } else {
                System.out.println("El token es inválido, el 'aud' no coincide, o ha expirado.");
                throw new IllegalArgumentException("El token es inválido, el 'aud' no coincide, o ha expirado.");
            }
            
        } catch (Exception e) {
            System.out.println("Error de seguridad o red al validar el token: " + e.getMessage());
            throw new IllegalArgumentException("Error de seguridad o red al validar el token: " + e.getMessage());
        }
    }
}