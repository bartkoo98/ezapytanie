package bartkoo98.com.github.ezapytanie.model;

import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "inquiries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inquiry {

    @Id
    private String id;

    private String title;
    private String description;
    private String category;
    private String deliveryLocation;
    private String termsAndConditions;

    @Indexed
    private String clientId;

    @Indexed
    private InquiryStatus status;

    @Indexed
    private Instant deadline;

    private String winnerOfferId;
    private String cancellationReason;
    private String selectionJustification;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
