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
import java.util.ArrayList;
import java.util.List;

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

    @Indexed
    private String clientId;

    @Indexed
    private InquiryStatus status;

    @Indexed
    private Instant deadline;

    @Indexed
    @Builder.Default
    private List<String> invitedContractorIds = new ArrayList<>();

    private String winnerOfferId;
    private String cancellationReason;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
