package bartkoo98.com.github.ezapytanie.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryQuestionResponse {
    private String id;
    private String questionText;
    private String answerText;
    private Instant createdAt;
    private Instant answeredAt;
}
