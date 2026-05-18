package bartkoo98.com.github.ezapytanie.repository;

import bartkoo98.com.github.ezapytanie.model.Offer;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OfferRepository extends MongoRepository<Offer, String> {

    boolean existsByInquiryIdAndContractorId(String inquiryId, String contractorId);

    List<Offer> findByInquiryId(String inquiryId);

    List<Offer> findByContractorId(String contractorId);
}
