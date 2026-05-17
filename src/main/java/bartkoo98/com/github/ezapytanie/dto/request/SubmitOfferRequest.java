package bartkoo98.com.github.ezapytanie.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitOfferRequest {

    @NotBlank
    private String inquiryId;

    @NotNull
    @Positive(message = "Total price must be greater than zero")
    private BigDecimal totalPrice;

    @NotBlank
    private String message;
}
