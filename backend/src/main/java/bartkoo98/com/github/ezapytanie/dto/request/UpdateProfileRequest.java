package bartkoo98.com.github.ezapytanie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    private String contactPhone;

    @NotBlank
    private String address;
}
