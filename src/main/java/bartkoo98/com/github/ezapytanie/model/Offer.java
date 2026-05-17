package bartkoo98.com.github.ezapytanie.model;

import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "offers")
@CompoundIndex(name = "unique_contractor_per_inquiry",
        def = "{'inquiryId': 1, 'contractorId': 1}",
        unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {

    @Id
    private String id;

    @Indexed
    private String inquiryId;

    @Indexed
    private String contractorId;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal totalPrice;

    private String message;

    private OfferStatus status;

    @CreatedDate
    private Instant submittedAt;

    @LastModifiedDate
    private Instant updatedAt;
}
