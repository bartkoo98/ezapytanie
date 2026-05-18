package bartkoo98.com.github.ezapytanie.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelInquiryRequest {

    @NotBlank(message = "Cancellation reason is required")
    private String cancellationReason;
}
