package bartkoo98.com.github.ezapytanie.dto.response;

import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private long expiresIn;
    private String email;
    private String role;
    private String fullName;
    private String institutionName;
    private CompanyDetails companyDetails;
}
