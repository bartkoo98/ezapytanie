package bartkoo98.com.github.ezapytanie.controller;

import bartkoo98.com.github.ezapytanie.dto.request.SubmitOfferRequest;
import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.service.OfferService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @PostMapping
    @PreAuthorize("hasRole('CONTRACTOR')")
    public ResponseEntity<OfferResponse> submit(
            @Valid @RequestBody SubmitOfferRequest request,
            HttpServletRequest httpRequest) {

        OfferResponse response = offerService.submit(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(201).body(response);
    }
}
