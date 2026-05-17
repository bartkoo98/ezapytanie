package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.CreateInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.exception.InvalidInquiryStateException;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.repository.OfferRepository;
import bartkoo98.com.github.ezapytanie.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final OfferRepository offerRepository;
    private final AuditLogService auditLogService;

    public InquiryResponse create(CreateInquiryRequest request, String ipAddress, String userAgent) {
        CustomUserDetails principal = getCurrentPrincipal();

        Inquiry inquiry = Inquiry.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .deadline(request.getDeadline())
                .status(InquiryStatus.DRAFT)
                .clientId(principal.getUserId())
                .build();

        Inquiry saved = inquiryRepository.save(inquiry);

        auditLogService.log(
                principal.getUserId(),
                principal.getUserRole().name(),
                principal.getUsername(),
                "INQUIRY_CREATED",
                "INQUIRY",
                saved.getId(),
                Map.of("title", saved.getTitle(), "category", saved.getCategory()),
                ipAddress,
                userAgent
        );

        return toResponse(saved);
    }

    public List<InquiryResponse> list() {
        CustomUserDetails principal = getCurrentPrincipal();

        List<Inquiry> inquiries = principal.getUserRole() == UserRole.ADMIN
                ? inquiryRepository.findAll()
                : inquiryRepository.findByClientId(principal.getUserId());

        return inquiries.stream().map(this::toResponse).toList();
    }

    /**
     * Transitions an inquiry from DRAFT to PUBLISHED, making it visible to contractors.
     * Only the owning CLIENT can publish their own inquiry.
     */
    @Transactional
    public InquiryResponse publish(String inquiryId, String ipAddress, String userAgent) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();
        if (!inquiry.getClientId().equals(principal.getUserId())) {
            throw new AccessDeniedException("You are not the owner of inquiry: " + inquiryId);
        }

        if (inquiry.getStatus() != InquiryStatus.DRAFT) {
            throw new InvalidInquiryStateException(inquiryId, inquiry.getStatus(), InquiryStatus.DRAFT);
        }

        inquiry.setStatus(InquiryStatus.PUBLISHED);
        Inquiry saved = inquiryRepository.save(inquiry);

        auditLogService.log(
                principal.getUserId(),
                principal.getUserRole().name(),
                principal.getUsername(),
                "INQUIRY_PUBLISHED",
                "INQUIRY",
                saved.getId(),
                Map.of("title", saved.getTitle()),
                ipAddress,
                userAgent
        );

        return toResponse(saved);
    }

    /**
     * Accepts one offer, batch-rejects all others, and closes the inquiry atomically.
     *
     * Requires MongoDB replica-set mode for true atomicity (@Transactional).
     * Ownership is verified here — @PostAuthorize is wrong for writes because it
     * runs after the mutation; @PreAuthorize cannot access the inquiry's clientId
     * without an extra query, so the check belongs in the service.
     */
    @Transactional
    public InquiryResponse acceptOffer(String inquiryId, String offerId,
                                       String ipAddress, String userAgent) {

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();
        if (!inquiry.getClientId().equals(principal.getUserId())) {
            throw new AccessDeniedException("You are not the owner of inquiry: " + inquiryId);
        }

        if (inquiry.getStatus() != InquiryStatus.PUBLISHED) {
            throw new InvalidInquiryStateException(inquiryId, inquiry.getStatus(), InquiryStatus.PUBLISHED);
        }

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getInquiryId().equals(inquiryId)) {
            throw new ResourceNotFoundException(
                    "Offer " + offerId + " does not belong to inquiry " + inquiryId);
        }

        offer.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(offer);

        List<Offer> otherOffers = offerRepository.findByInquiryId(inquiryId).stream()
                .filter(o -> !o.getId().equals(offerId))
                .toList();
        otherOffers.forEach(o -> o.setStatus(OfferStatus.REJECTED));
        offerRepository.saveAll(otherOffers);

        inquiry.setStatus(InquiryStatus.CLOSED);
        Inquiry saved = inquiryRepository.save(inquiry);

        String actorId = principal.getUserId();
        String actorRole = principal.getUserRole().name();
        String actorEmail = principal.getUsername();

        auditLogService.log(
                actorId, actorRole, actorEmail,
                "OFFER_ACCEPTED", "OFFER", offerId,
                Map.of("inquiryId", inquiryId, "acceptedOfferId", offerId),
                ipAddress, userAgent
        );

        auditLogService.log(
                actorId, actorRole, actorEmail,
                "INQUIRY_CLOSED", "INQUIRY", inquiryId,
                Map.of("acceptedOfferId", offerId,
                        "rejectedOfferCount", String.valueOf(otherOffers.size())),
                ipAddress, userAgent
        );

        return toResponse(saved);
    }

    private CustomUserDetails getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }

    private InquiryResponse toResponse(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .description(inquiry.getDescription())
                .category(inquiry.getCategory())
                .clientId(inquiry.getClientId())
                .status(inquiry.getStatus())
                .deadline(inquiry.getDeadline())
                .invitedContractorIds(inquiry.getInvitedContractorIds())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
