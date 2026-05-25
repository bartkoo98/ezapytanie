package bartkoo98.com.github.ezapytanie.mapper;

import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.model.User;
import org.springframework.stereotype.Component;

@Component
public class OfferResponseMapper {

    public OfferResponse toResponse(Offer offer, Inquiry inquiry) {
        boolean revealed = inquiry.getStatus() != InquiryStatus.PUBLISHED;
        return toResponse(offer, revealed, inquiry.getTitle(), null);
    }

    public OfferResponse toResponse(Offer offer, boolean revealed, String inquiryTitle) {
        return toResponse(offer, revealed, inquiryTitle, null);
    }

    public OfferResponse toResponse(Offer offer, boolean revealed, String inquiryTitle, User contractor) {
        return OfferResponse.builder()
                .id(offer.getId())
                .inquiryId(offer.getInquiryId())
                .inquiryTitle(inquiryTitle)
                .contractorId(offer.getContractorId())
                .contractorName(revealed ? offer.getContractorName() : null)
                .contractorInstitutionName(revealed && contractor != null ? contractor.getInstitutionName() : null)
                .contractorCompanyDetails(revealed && contractor != null ? contractor.getCompanyDetails() : null)
                .price(revealed ? offer.getPrice() : null)
                .currency(revealed ? offer.getCurrency() : null)
                .notes(revealed ? offer.getNotes() : null)
                .status(offer.getStatus())
                .submittedAt(offer.getSubmittedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }

    public OfferResponse toMaskedCompetitorResponse(Offer offer, String inquiryTitle) {
        return OfferResponse.builder()
                .id(offer.getId())
                .inquiryId(offer.getInquiryId())
                .inquiryTitle(inquiryTitle)
                .contractorId(null)
                .contractorName(null)
                .contractorInstitutionName("Anonimowy Wykonawca")
                .contractorCompanyDetails(null)
                .price(null)
                .currency(null)
                .notes("Zapieczętowane")
                .status(offer.getStatus())
                .submittedAt(offer.getSubmittedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }
}
