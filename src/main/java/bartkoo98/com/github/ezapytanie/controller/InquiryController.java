package bartkoo98.com.github.ezapytanie.controller;

import bartkoo98.com.github.ezapytanie.dto.request.CreateInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryResponse;
import bartkoo98.com.github.ezapytanie.service.InquiryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> create(
            @Valid @RequestBody CreateInquiryRequest request,
            HttpServletRequest httpRequest) {

        InquiryResponse created = inquiryService.create(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN')")
    public ResponseEntity<List<InquiryResponse>> list() {
        return ResponseEntity.ok(inquiryService.list());
    }

    @PatchMapping("/{inquiryId}/publish")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> publish(
            @PathVariable String inquiryId,
            HttpServletRequest httpRequest) {

        InquiryResponse updated = inquiryService.publish(
                inquiryId,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(updated);
    }

    /**
     * Accepts one offer for an inquiry, rejects all others, and closes the inquiry.
     * Role-level check (@PreAuthorize) gates entry; ownership check inside the service
     * prevents one CLIENT from closing another's inquiry.
     */
    @PatchMapping("/{inquiryId}/accept-offer/{offerId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> acceptOffer(
            @PathVariable String inquiryId,
            @PathVariable String offerId,
            HttpServletRequest httpRequest) {

        InquiryResponse updated = inquiryService.acceptOffer(
                inquiryId,
                offerId,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(updated);
    }
}
