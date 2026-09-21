package promo67.login.invent.auth;

import java.util.UUID;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.RequiredArgsConstructor;
import promo67.login.invent.jwt.GoogleTokenVerifierService;
import promo67.login.invent.jwt.JwtService;
import promo67.login.invent.models.Role;
import promo67.login.invent.models.User;
import promo67.login.invent.models.UserRepository;

@Service 
@RequiredArgsConstructor
public class AuthGoogleService {

    private final GoogleTokenVerifierService service;
    private final UserRepository repo;
    private final JwtService serv;
    private final PasswordEncoder encoder;

    public AuthResponse enter(GoogleLoginRequest request){
        GoogleIdToken.Payload payload = service.verifyToken(request.getToken());

        var user = repo.findByEmail(payload.getEmail());

        if (user.isPresent()) {
        UserDetails userD = user.get();

        return AuthResponse.builder()
                .token(serv.getToken(userD)) //
                .build();
    } else {
        User newUser = User.builder()
            .username(payload.getEmail())
            .password(encoder.encode(UUID.randomUUID().toString()))
            .email(payload.getEmail())
            .role(Role.USER)
            .build();

        repo.save(newUser);

        return AuthResponse.builder()
            .token(serv.getToken(newUser)).build();
        }
    }
}
