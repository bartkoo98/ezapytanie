package bartkoo98.com.github.ezapytanie.dto.response;

import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OfferResponse {

    private String id;
    private String inquiryId;
    private String contractorId;
    private BigDecimal totalPrice;
    private String message;
    private OfferStatus status;
    private Instant submittedAt;
}
