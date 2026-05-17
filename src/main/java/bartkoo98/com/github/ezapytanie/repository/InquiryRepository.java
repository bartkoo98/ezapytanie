package bartkoo98.com.github.ezapytanie.repository;

import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface InquiryRepository extends MongoRepository<Inquiry, String> {

    List<Inquiry> findByClientId(String clientId);

    List<Inquiry> findByStatus(InquiryStatus status);

    List<Inquiry> findByStatusAndDeadlineBefore(InquiryStatus status, Instant deadline);
}
