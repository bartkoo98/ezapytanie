package bartkoo98.com.github.ezapytanie.dto.response;

import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponse {

    private String id;
    private String title;
    private String description;
    private String category;
    private String clientId;
    private InquiryStatus status;
    private Instant deadline;
    private String winnerOfferId;
    private String cancellationReason;
    private String selectionJustification;
    private Instant createdAt;
    private Instant updatedAt;
}
