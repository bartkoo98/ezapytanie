package bartkoo98.com.github.ezapytanie.dto.request;

import bartkoo98.com.github.ezapytanie.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank
    private String fullName;

    @NotBlank
    private String institutionName;

    @NotNull
    private UserRole role;

    @NotBlank
    @Size(min = 10, max = 10, message = "NIP musi mieć dokładnie 10 cyfr")
    private String nip;

    @NotBlank
    @Size(min = 9, max = 14, message = "REGON musi mieć 9 lub 14 cyfr")
    private String regon;

    @NotBlank
    private String contactPhone;

    @NotBlank
    @Email
    private String contactEmail;

    @NotBlank
    private String address;
}
