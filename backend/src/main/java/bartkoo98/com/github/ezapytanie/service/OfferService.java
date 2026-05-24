package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.SubmitOfferRequest;
import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.exception.DuplicateOfferException;
import bartkoo98.com.github.ezapytanie.exception.InquiryNotOpenException;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.mapper.OfferResponseMapper;
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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepository;
    private final InquiryRepository inquiryRepository;
    private final AuditLogService auditLogService;
    private final OfferResponseMapper mapper;

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
                .contractorName(principal.getFullName())
                .price(request.getPrice())
                .notes(request.getNotes())
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
                Map.of("inquiryId", request.getInquiryId(),
                        "price", request.getPrice().toPlainString()),
                ipAddress,
                userAgent
        );

        return mapper.toResponse(saved, inquiry);
    }

    public List<OfferResponse> listForInquiry(String inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();

        return switch (principal.getUserRole()) {
            case ADMIN -> offerRepository.findByInquiryId(inquiryId).stream()
                    .map(o -> mapper.toResponse(o, true, inquiry.getTitle()))
                    .toList();

            case CLIENT -> {
                if (!inquiry.getClientId().equals(principal.getUserId())) {
                    throw new AccessDeniedException("You are not the owner of inquiry: " + inquiryId);
                }
                // Sealed while PUBLISHED; revealed once closed/cancelled/archived
                yield offerRepository.findByInquiryId(inquiryId).stream()
                        .map(o -> mapper.toResponse(o, inquiry))
                        .toList();
            }

            case CONTRACTOR -> {
                yield offerRepository.findByInquiryId(inquiryId).stream()
                        .filter(o -> o.getContractorId().equals(principal.getUserId()))
                        .map(o -> mapper.toResponse(o, true, inquiry.getTitle()))
                        .toList();
            }
        };
    }

    public List<OfferResponse> listMine() {
        CustomUserDetails principal = getCurrentPrincipal();
        List<Offer> offers = offerRepository.findByContractorId(principal.getUserId());
        Set<String> inquiryIds = offers.stream().map(Offer::getInquiryId).collect(Collectors.toSet());
        Map<String, String> titleById = inquiryRepository.findAllById(inquiryIds).stream()
                .collect(Collectors.toMap(Inquiry::getId, Inquiry::getTitle));
        return offers.stream()
                .map(o -> mapper.toResponse(o, true, titleById.getOrDefault(o.getInquiryId(), o.getInquiryId())))
                .toList();
    }

    private CustomUserDetails getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (CustomUserDetails) auth.getPrincipal();
    }
}
