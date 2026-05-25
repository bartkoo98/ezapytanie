package bartkoo98.com.github.ezapytanie.controller;

import bartkoo98.com.github.ezapytanie.dto.request.AcceptOfferRequest;
import bartkoo98.com.github.ezapytanie.dto.request.AnswerQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.request.AskQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.request.CancelInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.request.CreateInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryQuestionResponse;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryResponse;
import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.dto.response.PageResponse;
import bartkoo98.com.github.ezapytanie.service.InquiryQuestionService;
import bartkoo98.com.github.ezapytanie.service.InquiryService;
import bartkoo98.com.github.ezapytanie.service.OfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final OfferService offerService;
    private final InquiryQuestionService questionService;

    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> create(@Valid @RequestBody CreateInquiryRequest request) {
        return ResponseEntity.status(201).body(inquiryService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'CONTRACTOR')")
    public ResponseEntity<PageResponse<InquiryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(inquiryService.list(pageable));
    }

    @GetMapping("/{inquiryId}")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'CONTRACTOR')")
    public ResponseEntity<InquiryResponse> getById(@PathVariable String inquiryId) {
        return ResponseEntity.ok(inquiryService.getById(inquiryId));
    }

    @PatchMapping("/{inquiryId}/cancel")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> cancel(
            @PathVariable String inquiryId,
            @Valid @RequestBody CancelInquiryRequest request) {
        return ResponseEntity.ok(inquiryService.cancel(inquiryId, request));
    }

    @GetMapping("/{inquiryId}/offers")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'CONTRACTOR')")
    public ResponseEntity<List<OfferResponse>> listOffers(@PathVariable String inquiryId) {
        return ResponseEntity.ok(offerService.listForInquiry(inquiryId));
    }

    @PatchMapping("/{inquiryId}/accept-offer/{offerId}")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryResponse> acceptOffer(
            @PathVariable String inquiryId,
            @PathVariable String offerId,
            @RequestBody AcceptOfferRequest request) {
        return ResponseEntity.ok(inquiryService.acceptOffer(
                inquiryId, offerId, request.getSelectionJustification()));
    }

    @PostMapping("/{inquiryId}/questions")
    @PreAuthorize("hasRole('CONTRACTOR')")
    public ResponseEntity<InquiryQuestionResponse> askQuestion(
            @PathVariable String inquiryId,
            @Valid @RequestBody AskQuestionRequest request) {
        return ResponseEntity.status(201).body(questionService.ask(inquiryId, request));
    }

    @PutMapping("/{inquiryId}/questions/{questionId}/answer")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<InquiryQuestionResponse> answerQuestion(
            @PathVariable String inquiryId,
            @PathVariable String questionId,
            @Valid @RequestBody AnswerQuestionRequest request) {
        return ResponseEntity.ok(questionService.answer(inquiryId, questionId, request));
    }

    @GetMapping("/{inquiryId}/questions")
    @PreAuthorize("hasAnyRole('CLIENT', 'ADMIN', 'CONTRACTOR')")
    public ResponseEntity<List<InquiryQuestionResponse>> listQuestions(@PathVariable String inquiryId) {
        return ResponseEntity.ok(questionService.listForInquiry(inquiryId));
    }
}
