package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.LoginRequest;
import bartkoo98.com.github.ezapytanie.dto.request.RegisterRequest;
import bartkoo98.com.github.ezapytanie.dto.response.LoginResponse;
import bartkoo98.com.github.ezapytanie.exception.EmailAlreadyExistsException;
import bartkoo98.com.github.ezapytanie.exception.InvalidCredentialsException;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.UserRepository;
import bartkoo98.com.github.ezapytanie.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Registers a new user account. Throws {@link EmailAlreadyExistsException} if
     * the email is already taken (caller should map this to HTTP 409).
     */
    public User register(RegisterRequest request, String ipAddress, String userAgent) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .institutionName(request.getInstitutionName())
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        auditLogService.log(
                saved.getId(),
                saved.getRole().name(),
                saved.getEmail(),
                "USER_REGISTERED",
                "USER",
                saved.getId(),
                Map.of("fullName", saved.getFullName(), "institutionName", saved.getInstitutionName()),
                ipAddress,
                userAgent
        );

        return saved;
    }

    /**
     * Authenticates a user and returns JWT access and refresh tokens.
     * Throws {@link InvalidCredentialsException} for any credential mismatch
     * (intentionally generic to avoid email enumeration).
     */
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new InvalidCredentialsException();
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);

        auditLogService.log(
                user.getId(),
                user.getRole().name(),
                user.getEmail(),
                "USER_LOGIN_SUCCESS",
                "USER",
                user.getId(),
                null,
                ipAddress,
                userAgent
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .institutionName(user.getInstitutionName())
                .build();
    }
}
