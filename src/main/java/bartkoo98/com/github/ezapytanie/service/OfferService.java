package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.SubmitOfferRequest;
import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.exception.DuplicateOfferException;
import bartkoo98.com.github.ezapytanie.exception.InquiryNotOpenException;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.repository.OfferRepository;
import bartkoo98.com.github.ezapytanie.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final InquiryRepository inquiryRepository;
    private final AuditLogService auditLogService;

    public OfferResponse submit(SubmitOfferRequest request, String ipAddress, String userAgent) {
        Inquiry inquiry = inquiryRepository.findById(request.getInquiryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inquiry not found: " + request.getInquiryId()));

        if (inquiry.getStatus() != InquiryStatus.PUBLISHED) {
            throw new InquiryNotOpenException(request.getInquiryId());
        }

        CustomUserDetails principal = getCurrentPrincipal();
        String contractorId = principal.getUserId();

        if (offerRepository.existsByInquiryIdAndContractorId(request.getInquiryId(), contractorId)) {
            throw new DuplicateOfferException(request.getInquiryId());
        }

        Offer offer = Offer.builder()
                .inquiryId(request.getInquiryId())
                .contractorId(contractorId)
                .totalPrice(request.getTotalPrice())
                .message(request.getMessage())
                .status(OfferStatus.SUBMITTED)
                .build();

        Offer saved = offerRepository.save(offer);

        auditLogService.log(
                contractorId,
                principal.getUserRole().name(),
                principal.getUsername(),
                "OFFER_SUBMITTED",
                "OFFER",
                saved.getId(),
                Map.of(
                        "inquiryId", request.getInquiryId(),
                        "totalPrice", request.getTotalPrice().toPlainString()
                ),
                ipAddress,
                userAgent
        );

        return toResponse(saved);
    }

    private CustomUserDetails getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }

    private OfferResponse toResponse(Offer offer) {
        return OfferResponse.builder()
                .id(offer.getId())
                .inquiryId(offer.getInquiryId())
                .contractorId(offer.getContractorId())
                .totalPrice(offer.getTotalPrice())
                .message(offer.getMessage())
                .status(offer.getStatus())
                .submittedAt(offer.getSubmittedAt())
                .build();
    }
}
