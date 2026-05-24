package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.AnswerQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.request.AskQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryQuestionResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.InquiryQuestion;
import bartkoo98.com.github.ezapytanie.repository.InquiryQuestionRepository;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryQuestionService {

    private final InquiryQuestionRepository questionRepository;
    private final InquiryRepository inquiryRepository;

    public InquiryQuestionResponse ask(String inquiryId, AskQuestionRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        if (inquiry.getStatus() != InquiryStatus.PUBLISHED) {
            throw new AccessDeniedException("Questions can only be posted on active inquiries");
        }

        CustomUserDetails principal = getCurrentPrincipal();

        InquiryQuestion question = InquiryQuestion.builder()
                .inquiryId(inquiryId)
                .authorId(principal.getUserId())
                .questionText(request.getQuestionText())
                .build();

        return toResponse(questionRepository.save(question));
    }

    public InquiryQuestionResponse answer(String inquiryId, String questionId, AnswerQuestionRequest request) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("Inquiry not found: " + inquiryId));

        CustomUserDetails principal = getCurrentPrincipal();
        if (!inquiry.getClientId().equals(principal.getUserId())) {
            throw new AccessDeniedException("Only the inquiry owner can answer questions");
        }

        InquiryQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (!question.getInquiryId().equals(inquiryId)) {
            throw new ResourceNotFoundException("Question " + questionId + " does not belong to inquiry " + inquiryId);
        }

        question.setAnswerText(request.getAnswerText());
        question.setAnsweredAt(Instant.now());

        return toResponse(questionRepository.save(question));
    }

    public List<InquiryQuestionResponse> listForInquiry(String inquiryId) {
        if (!inquiryRepository.existsById(inquiryId)) {
            throw new ResourceNotFoundException("Inquiry not found: " + inquiryId);
        }
        return questionRepository.findByInquiryIdOrderByCreatedAtAsc(inquiryId).stream()
                .map(this::toResponse)
                .toList();
    }

    private InquiryQuestionResponse toResponse(InquiryQuestion q) {
        return InquiryQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .answerText(q.getAnswerText())
                .createdAt(q.getCreatedAt())
                .answeredAt(q.getAnsweredAt())
                .build();
    }

    private CustomUserDetails getCurrentPrincipal() {
        return (CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }
}
