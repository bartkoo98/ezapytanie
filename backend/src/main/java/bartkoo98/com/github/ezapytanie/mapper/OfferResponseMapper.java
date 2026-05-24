package bartkoo98.com.github.ezapytanie.mapper;

import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import org.springframework.stereotype.Component;

/**
 * Single source of truth for offer visibility.
 * Price, contractor identity, and offer details are masked while the inquiry is PUBLISHED.
 * Once the inquiry moves to any other status the bids are considered revealed.
 */
@Component
public class OfferResponseMapper {

    public OfferResponse toResponse(Offer offer, Inquiry inquiry) {
        boolean revealed = inquiry.getStatus() != InquiryStatus.PUBLISHED;
        return toResponse(offer, revealed, inquiry.getTitle());
    }

    public OfferResponse toResponse(Offer offer, boolean revealed, String inquiryTitle) {
        return OfferResponse.builder()
                .id(offer.getId())
                .inquiryId(offer.getInquiryId())
                .inquiryTitle(inquiryTitle)
                .contractorId(offer.getContractorId())
                .contractorName(revealed ? offer.getContractorName() : null)
                .price(revealed ? offer.getPrice() : null)
                .currency(revealed ? offer.getCurrency() : null)
                .notes(revealed ? offer.getNotes() : null)
                .status(offer.getStatus())
                .submittedAt(offer.getSubmittedAt())
                .updatedAt(offer.getUpdatedAt())
                .build();
    }
}
