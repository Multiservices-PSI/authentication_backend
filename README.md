# login.invent  ✅

API de autenticación para usuarios con Spring Boot, Spring Security, Spring Data JPA, MySQL, JWT y login con Google.

## Descripción

Este proyecto expone endpoints para:

- Registro de usuarios con correo, username y contraseña.
- Inicio de sesión local con username o email + password.
- Emisión y validación de JWT para proteger rutas internas.
- Inicio de sesión con Google usando un ID token verificado por Google.
- Protección de rutas mediante Spring Security con sesión stateless.

## Requisitos

- Java JDK 25.
- MySQL Server ejecutándose en localhost:3306.
- Maven Wrapper disponible en la raíz del proyecto (`mvnw.cmd` en Windows o `./mvnw` en Linux/macOS).
- Cliente OAuth de Google configurado en Google Cloud Console.

La aplicación usa Spring Boot 4.1.1 y crea la base de datos `multiservicespsidb` automáticamente si MySQL tiene permisos para hacerlo.

## Configuración de MySQL

La conexión está definida en `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/multiservicespsidb?createDatabaseIfNotExist=true
spring.datasource.username=${userMySQL}
spring.datasource.password=${passMySQL}
```

Por lo tanto:

- Host: localhost
- Puerto: 3306
- Base de datos: multiservicespsidb
- Usuario: variable de entorno `userMySQL`
- Contraseña: variable de entorno `passMySQL`

No es necesario crear la base de datos manualmente porque la URL incluye `createDatabaseIfNotExist=true`.

## Variables de entorno

Antes de iniciar la aplicación, define estas variables:

| Variable | Uso |
| --- | --- |
| `userMySQL` | Usuario de MySQL |
| `passMySQL` | Contraseña de MySQL |
| `claveJWT` | Clave secreta usada para firmar JWT codificada en Base64 |
| `idCliente` | ID del cliente de Google OAuth |

### Ejemplo en PowerShell

```powershell
$env:userMySQL = "root"
$env:passMySQL = "tu_contrasena"
$env:claveJWT = "VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdCYXNlNjRTZWNyZXRLZXlGb3JKV1Q="
$env:idCliente = "347591718340-dunkdou7s62shqfljii7bigebid1fbut.apps.googleusercontent.com"
```

### Ejemplo en CMD

```bat
set userMySQL=root
set passMySQL=tu_contrasena
set claveJWT=VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdCYXNlNjRTZWNyZXRLZXlGb3JKV1Q=
set idCliente=347591718340-dunkdou7s62shqfljii7bigebid1fbut.apps.googleusercontent.com
```

### Ejemplo en Linux/macOS

```bash
export userMySQL=root
export passMySQL='tu_contrasena'
export claveJWT='VGhpc0lzQVN1ZmZpY2llbnRseUxvbmdCYXNlNjRTZWNyZXRLZXlGb3JKV1Q='
export idCliente='347591718340-dunkdou7s62shqfljii7bigebid1fbut.apps.googleusercontent.com'
```

> La clave JWT debe ser una cadena Base64 válida y suficientemente larga para HMAC-SHA. El valor de `idCliente` debe coincidir con el Client ID de Google configurado para la app.

## Ejecutar el proyecto

Desde la carpeta raíz de `login.invent`:

### Windows PowerShell

```powershell
./mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

La API queda disponible en:

```text
http://localhost:8080
```

Para compilar y ejecutar el JAR:

```powershell
./mvnw.cmd clean package
java -jar target/login.invent-0.0.1-SNAPSHOT.jar
```

En Linux/macOS, usa `./mvnw` en lugar de `./mvnw.cmd`.

## Ejecutar pruebas

```powershell
./mvnw.cmd test
```

Las pruebas de contexto necesitan que existan las variables de entorno MySQL y JWT para que Spring pueda crear todos los beans de seguridad y autenticación.

## Seguridad

La configuración actual permite acceso público a todas las rutas bajo `/auth/**` y protege el resto de endpoints con JWT:

```java
.requestMatchers("/auth/**").permitAll()
.anyRequest().authenticated()
```

Las sesiones se manejan como `STATELESS`, por lo que la autenticación se basa en el token enviado en cada petición.

## API de autenticación

Todas las peticiones usan JSON y la base es `http://localhost:8080`.

### 1) Registrar usuario

`POST /auth/register`

```bash
curl -X POST http://localhost:8080/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"ana","email":"ana@example.com","password":"Secret123!"}'
```

Respuesta esperada:

```json
{
  "token": "eyJ..."
}
```

Si el username o email ya existen, la aplicación lanza una excepción y la respuesta de error se maneja globalmente.

### 2) Iniciar sesión con username o email

`POST /auth/login`

Ejemplo con username:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ana","password":"Secret123!"}'
```

Ejemplo con email:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@example.com","password":"Secret123!"}'
```

Respuesta:

```json
{
  "token": "eyJ..."
}
```

### 3) Iniciar sesión con Google

`POST /auth/Google`

```bash
curl -X POST http://localhost:8080/auth/Google \
  -H "Content-Type: application/json" \
  -d '{"token":"<google_id_token>"}'
```

El servicio valida el token con Google, comprueba el `aud` con el `Client ID` configurado y, si es válido, crea o reutiliza el usuario y devuelve un JWT interno del sistema.

### 4) Uso de rutas protegidas

Las rutas que no sean `/auth/**` requieren el token JWT:

```text
Authorization: Bearer <token>
```

Ejemplo:

```bash
curl -X GET http://localhost:8080/alguna-ruta-protegida \
  -H "Authorization: Bearer <token>"
```

## Estructura del proyecto

```text
src/main/java/promo67/login/invent/
├── Application.java          # Punto de entrada
├── auth/                     # Controlador, DTOs y servicios de autenticación
├── config/                   # Configuración de seguridad y beans de Spring
├── exception/                # Manejo global de excepciones
├── jwt/                      # Generación, validación y filtrado de JWT
├── models/                   # Entidad User, enum Role y repositorio
└── security-related files
```

## Notas importantes

- `spring.jpa.hibernate.ddl-auto=update` actualiza la estructura de la base de datos al iniciar.
- `spring.jpa.show-sql=true` imprime consultas SQL en la consola.
- Las contraseñas se almacenan con BCrypt.
- El proyecto usa JWT con expiración aproximada de 24 minutos.
- El login con Google depende del valor de `spring.security.oauth2.client.registration.google.client-id`.
- No se deben incluir credenciales reales ni secretos en el repositorio.

## Variables y configuración actuales relevantes

En `application.properties` se tienen configuradas estas propiedades:

```properties
server.port=8080
spring.jpa.hibernate.ddl-auto=update
spring.datasource.url=jdbc:mysql://localhost:3306/multiservicespsidb?createDatabaseIfNotExist=true
spring.datasource.username=${userMySQL}
spring.datasource.password=${passMySQL}
config.secretKey=${claveJWT}
spring.security.oauth2.client.registration.google.client-id=${idCliente}
```

Esto refleja el estado actual del proyecto y sus cambios recientes con autenticación por JWT y Google.
