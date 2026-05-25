package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.LoginRequest;
import bartkoo98.com.github.ezapytanie.dto.request.RegisterRequest;
import bartkoo98.com.github.ezapytanie.dto.response.LoginResponse;
import bartkoo98.com.github.ezapytanie.exception.EmailAlreadyExistsException;
import bartkoo98.com.github.ezapytanie.exception.InvalidCredentialsException;
import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
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

    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        CompanyDetails companyDetails = CompanyDetails.builder()
                .nip(request.getNip())
                .regon(request.getRegon())
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .address(request.getAddress())
                .build();

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .institutionName(request.getInstitutionName())
                .companyDetails(companyDetails)
                .role(request.getRole())
                .active(true)
                .build();

        User saved = userRepository.save(user);

        auditLogService.log(
                saved.getId(), saved.getRole().name(), saved.getEmail(),
                "USER_REGISTERED", "USER", saved.getId(),
                Map.of("fullName", saved.getFullName(), "institutionName", saved.getInstitutionName())
        );

        return saved;
    }

    public LoginResponse login(LoginRequest request) {
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
                user.getId(), user.getRole().name(), user.getEmail(),
                "USER_LOGIN_SUCCESS", "USER", user.getId(),
                null
        );

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .email(user.getEmail())
                .role(user.getRole().name())
                .fullName(user.getFullName())
                .institutionName(user.getInstitutionName())
                .companyDetails(user.getCompanyDetails())
                .build();
    }
}
