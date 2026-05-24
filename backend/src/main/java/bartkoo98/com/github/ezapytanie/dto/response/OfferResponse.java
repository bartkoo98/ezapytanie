package bartkoo98.com.github.ezapytanie.dto.response;

import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
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
    private String inquiryTitle;
    private String contractorId;
    private String contractorName;
    private String contractorInstitutionName;
    private CompanyDetails contractorCompanyDetails;
    private BigDecimal price;
    private String currency;
    private String notes;
    private OfferStatus status;
    private Instant submittedAt;
    private Instant updatedAt;
}
