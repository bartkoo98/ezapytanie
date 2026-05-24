package bartkoo98.com.github.ezapytanie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "inquiry_questions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryQuestion {

    @Id
    private String id;

    @Indexed
    private String inquiryId;

    private String authorId;

    private String questionText;
    private String answerText;

    @CreatedDate
    private Instant createdAt;

    private Instant answeredAt;
}
