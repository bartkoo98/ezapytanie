package bartkoo98.com.github.ezapytanie.repository;

import bartkoo98.com.github.ezapytanie.model.InquiryQuestion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface InquiryQuestionRepository extends MongoRepository<InquiryQuestion, String> {
    List<InquiryQuestion> findByInquiryIdOrderByCreatedAtAsc(String inquiryId);
}
