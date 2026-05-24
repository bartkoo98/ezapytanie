package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.CancelInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.request.CreateInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryResponse;
import bartkoo98.com.github.ezapytanie.dto.response.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
                .deliveryLocation(request.getDeliveryLocation())
                .termsAndConditions(request.getTermsAndConditions())
                .deadline(request.getDeadline())
                .status(InquiryStatus.PUBLISHED)
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

        return toResponse(saved, true);
    }

    public PageResponse<InquiryResponse> list(Pageable pageable) {
        CustomUserDetails principal = getCurrentPrincipal();

        Page<Inquiry> inquiryPage = switch (principal.getUserRole()) {
            case ADMIN -> inquiryRepository.findAll(pageable);
            case CLIENT -> inquiryRepository.findByClientId(principal.getUserId(), pageable);
            case CONTRACTOR -> inquiryRepository.findByStatusIn(
                    List.of(InquiryStatus.PUBLISHED, InquiryStatus.CLOSED, InquiryStatus.ARCHIVED), pageable);
        };

        boolean includeJustification = principal.getUserRole() != UserRole.CONTRACTOR;
        List<InquiryResponse> content = inquiryPage.getContent().stream()
                .map(i -> toResponse(i, includeJustification)).toList();
        return new PageResponse<>(content, inquiryPage.getNumber(), inquiryPage.getTotalPages(), inquiryPage.getTotalElements());
    }

    public InquiryResponse getById(String inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();

        switch (principal.getUserRole()) {
            case ADMIN -> { /* full access */ }
            case CLIENT -> {
                if (!inquiry.getClientId().equals(principal.getUserId())) {
                    throw new AccessDeniedException("You are not the owner of inquiry: " + inquiryId);
                }
            }
            case CONTRACTOR -> {
                if (inquiry.getStatus() == InquiryStatus.CANCELLED) {
                    throw new AccessDeniedException("Inquiry is not available: " + inquiryId);
                }
            }
        }

        return toResponse(inquiry, principal.getUserRole() != UserRole.CONTRACTOR);
    }

    @Transactional
    public InquiryResponse cancel(String inquiryId, CancelInquiryRequest request,
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

        List<Offer> submittedOffers = offerRepository.findByInquiryId(inquiryId).stream()
                .filter(o -> o.getStatus() == OfferStatus.SUBMITTED)
                .toList();
        submittedOffers.forEach(o -> o.setStatus(OfferStatus.REJECTED));
        offerRepository.saveAll(submittedOffers);

        inquiry.setStatus(InquiryStatus.CANCELLED);
        inquiry.setCancellationReason(request.getCancellationReason());
        Inquiry saved = inquiryRepository.save(inquiry);

        auditLogService.log(
                principal.getUserId(),
                principal.getUserRole().name(),
                principal.getUsername(),
                "INQUIRY_CANCELLED",
                "INQUIRY",
                saved.getId(),
                Map.of("reason", request.getCancellationReason(),
                        "rejectedOfferCount", String.valueOf(submittedOffers.size())),
                ipAddress,
                userAgent
        );

        return toResponse(saved, true);
    }

    @Transactional
    public InquiryResponse acceptOffer(String inquiryId, String offerId, String selectionJustification,
                                       String ipAddress, String userAgent) {

        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();
        if (!inquiry.getClientId().equals(principal.getUserId())) {
            throw new AccessDeniedException("You are not the owner of inquiry: " + inquiryId);
        }

        if (inquiry.getStatus() != InquiryStatus.CLOSED) {
            throw new InvalidInquiryStateException(inquiryId, inquiry.getStatus(), InquiryStatus.CLOSED);
        }

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found: " + offerId));

        if (!offer.getInquiryId().equals(inquiryId)) {
            throw new ResourceNotFoundException(
                    "Offer " + offerId + " does not belong to inquiry " + inquiryId);
        }

        offer.setStatus(OfferStatus.SELECTED);
        offerRepository.save(offer);

        List<Offer> otherOffers = offerRepository.findByInquiryId(inquiryId).stream()
                .filter(o -> !o.getId().equals(offerId))
                .toList();
        otherOffers.forEach(o -> o.setStatus(OfferStatus.REJECTED));
        offerRepository.saveAll(otherOffers);

        inquiry.setStatus(InquiryStatus.ARCHIVED);
        inquiry.setWinnerOfferId(offerId);
        inquiry.setSelectionJustification(selectionJustification);
        Inquiry saved = inquiryRepository.save(inquiry);

        String actorId = principal.getUserId();
        String actorRole = principal.getUserRole().name();
        String actorEmail = principal.getUsername();

        auditLogService.log(
                actorId, actorRole, actorEmail,
                "OFFER_SELECTED", "OFFER", offerId,
                Map.of("inquiryId", inquiryId, "selectedOfferId", offerId),
                ipAddress, userAgent
        );

        auditLogService.log(
                actorId, actorRole, actorEmail,
                "INQUIRY_ARCHIVED", "INQUIRY", inquiryId,
                Map.of("winnerOfferId", offerId,
                        "rejectedOfferCount", String.valueOf(otherOffers.size())),
                ipAddress, userAgent
        );

        return toResponse(saved, true);
    }

    private CustomUserDetails getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }

    private InquiryResponse toResponse(Inquiry inquiry, boolean includeJustification) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .description(inquiry.getDescription())
                .category(inquiry.getCategory())
                .deliveryLocation(inquiry.getDeliveryLocation())
                .termsAndConditions(inquiry.getTermsAndConditions())
                .clientId(inquiry.getClientId())
                .status(inquiry.getStatus())
                .deadline(inquiry.getDeadline())
                .winnerOfferId(inquiry.getWinnerOfferId())
                .cancellationReason(inquiry.getCancellationReason())
                .selectionJustification(includeJustification ? inquiry.getSelectionJustification() : null)
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
