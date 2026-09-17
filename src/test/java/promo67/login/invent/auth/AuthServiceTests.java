package promo67.login.invent.auth;

import java.util.Collections;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

import promo67.login.invent.jwt.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class AuthServiceTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthenticationManager authenticationManager;

    @MockitoBean
    private JwtService jwtService;

    // Token JWT simulado (formato válido de 3 partes separadas por puntos)
    private final String MOCK_JWT = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
            "eyJzdWIiOiJhZG1pbiIsImlhdCI6MTUxNjIzOTAyMn0." +
            "SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @Nested
    @DisplayName("Criterio: Camino Feliz (Happy Path)")
    class HappyPathTests {

        @Test
        @DisplayName("Debe retornar 200 OK y token JWT válido con credenciales correctas")
        void shouldReturnOkAndValidJwt() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "Password123*", "admin@promo67.com");

            UserDetails userDetails = new User("admin", "Password123*", Collections.emptyList());
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.getToken(any())).thenReturn(MOCK_JWT);

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.token", notNullValue()))
                    .andExpect(jsonPath("$.token", matchesPattern("^[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_=]+\\.[A-Za-z0-9-_.+/=]*$")));
        }
    }

    @Nested
    @DisplayName("Criterio: Credenciales Inválidas")
    class InvalidCredentialsTests {

        @Test
        @DisplayName("Debe retornar 401 Unauthorized cuando la contraseña o usuario son erróneos")
        void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
            // Arrange
            LoginRequest invalidRequest = new LoginRequest("admin", "PasswordErronea", "admin@promo67.com");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Credenciales incorrectas"));

            // Act & Assert
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isUnauthorized()); 
                    // Nota: Cambiar a .isForbidden() si tu configuración de Spring Security mapea el error a 403
        }
    }

    @Nested
    @DisplayName("Criterio: Validación de Datos")
    class DataValidationTests {

        @Test
        @DisplayName("Debe retornar 400 Bad Request cuando el payload está vacío")
        void shouldReturnBadRequestWhenPayloadIsEmpty() throws Exception {
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 Bad Request cuando falta el campo password")
        void shouldReturnBadRequestWhenPasswordIsMissing() throws Exception {
            LoginRequest requestWithoutPassword = new LoginRequest();
            requestWithoutPassword.setUsername("admin");
            requestWithoutPassword.setPassword(null);

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutPassword)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 Bad Request cuando el password es una cadena en blanco")
        void shouldReturnBadRequestWhenPasswordIsBlank() throws Exception {
            LoginRequest blankPasswordRequest = new LoginRequest("admin", "   ", "admin@promo67.com");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(blankPasswordRequest)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Debe retornar 400 Bad Request cuando falta el username")
        void shouldReturnBadRequestWhenUsernameIsMissing() throws Exception {
            LoginRequest requestWithoutUser = new LoginRequest();
            requestWithoutUser.setUsername(null);
            requestWithoutUser.setPassword("Password123*");

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithoutUser)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Criterio: Generación de JWT")
    class JwtGenerationTests {

        @Test
        @DisplayName("Debe invocar a JwtService y asegurar que el token generado no sea nulo")
        void shouldVerifyJwtServiceInvocationAndIntegrity() throws Exception {
            // Arrange
            LoginRequest request = new LoginRequest("admin", "Password123*", "admin@promo67.com");

            UserDetails userDetails = new User("admin", "Password123*", Collections.emptyList());
            Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtService.getToken(any())).thenReturn(MOCK_JWT);

            // Act
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Assert: Se verifica la interacción con el servicio JWT
            verify(jwtService, times(1)).getToken(any());
        }
    }
}