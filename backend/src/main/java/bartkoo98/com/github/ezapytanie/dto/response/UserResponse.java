package bartkoo98.com.github.ezapytanie.dto.response;

import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private String id;
    private String email;
    private String fullName;
    private String institutionName;
    private CompanyDetails companyDetails;
    private UserRole role;
    private boolean active;
    private Instant createdAt;
    private Instant lastLoginAt;
}
