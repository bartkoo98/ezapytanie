package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.AnswerQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.request.AskQuestionRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryQuestionResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.InquiryQuestion;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.InquiryQuestionRepository;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryQuestionServiceTest {

    @Mock private InquiryQuestionRepository questionRepository;
    @Mock private InquiryRepository inquiryRepository;

    @InjectMocks
    private InquiryQuestionService questionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockPrincipal(String userId, UserRole role) {
        User user = User.builder()
                .id(userId).fullName("Test User").role(role).email(userId + "@test.pl")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private Inquiry publishedInquiry(String id, String clientId) {
        return Inquiry.builder().id(id).clientId(clientId).status(InquiryStatus.PUBLISHED).build();
    }

    private AskQuestionRequest askRequest(String text) {
        AskQuestionRequest req = new AskQuestionRequest();
        req.setQuestionText(text);
        return req;
    }

    private AnswerQuestionRequest answerRequest(String text) {
        AnswerQuestionRequest req = new AnswerQuestionRequest();
        req.setAnswerText(text);
        return req;
    }

    @Test
    void ask_whenInquiryPublished_savesQuestionWithAuthorId() {
        // Arrange
        mockPrincipal("contractor-1", UserRole.CONTRACTOR);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));
        InquiryQuestion saved = InquiryQuestion.builder()
                .id("q-1").inquiryId("inquiry-1").authorId("contractor-1")
                .questionText("Jaki jest termin dostawy?")
                .build();
        when(questionRepository.save(any())).thenReturn(saved);

        // Act
        InquiryQuestionResponse result = questionService.ask("inquiry-1", askRequest("Jaki jest termin dostawy?"));

        // Assert — question persisted with correct author
        assertThat(result.getId()).isEqualTo("q-1");

        ArgumentCaptor<InquiryQuestion> captor = ArgumentCaptor.forClass(InquiryQuestion.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getAuthorId()).isEqualTo("contractor-1");
        assertThat(captor.getValue().getInquiryId()).isEqualTo("inquiry-1");
        assertThat(captor.getValue().getQuestionText()).isEqualTo("Jaki jest termin dostawy?");
    }

    @Test
    void ask_whenInquiryNotPublished_throwsAccessDeniedException() {
        // Arrange — principal not needed: exception thrown before getCurrentPrincipal()
        Inquiry closed = Inquiry.builder().id("inquiry-1").clientId("client-1")
                .status(InquiryStatus.CLOSED).build();
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(closed));

        // Act & Assert
        assertThatThrownBy(() -> questionService.ask("inquiry-1", askRequest("Pytanie")))
                .isInstanceOf(AccessDeniedException.class);

        verify(questionRepository, never()).save(any());
    }

    @Test
    void ask_whenInquiryNotFound_throwsResourceNotFoundException() {
        // Arrange — principal not needed: exception thrown before getCurrentPrincipal()
        when(inquiryRepository.findById("missing")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> questionService.ask("missing", askRequest("Pytanie")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("missing");

        verify(questionRepository, never()).save(any());
    }


    @Test
    void answer_asOwner_setsAnswerTextAndTimestamp() {
        // Arrange
        mockPrincipal("client-1", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));
        InquiryQuestion question = InquiryQuestion.builder()
                .id("q-1").inquiryId("inquiry-1").questionText("Pytanie?")
                .build();
        when(questionRepository.findById("q-1")).thenReturn(Optional.of(question));
        when(questionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        questionService.answer("inquiry-1", "q-1", answerRequest("Odpowiedź właściciela"));

        // Assert — answer text set, timestamp populated
        ArgumentCaptor<InquiryQuestion> captor = ArgumentCaptor.forClass(InquiryQuestion.class);
        verify(questionRepository).save(captor.capture());
        InquiryQuestion persisted = captor.getValue();
        assertThat(persisted.getAnswerText()).isEqualTo("Odpowiedź właściciela");
        assertThat(persisted.getAnsweredAt()).isNotNull();
    }

    @Test
    void answer_asNonOwner_throwsAccessDeniedException() {
        // Arrange — a different client tries to answer
        mockPrincipal("other-client", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));

        // Act & Assert
        assertThatThrownBy(() -> questionService.answer("inquiry-1", "q-1", answerRequest("Odpowiedź")))
                .isInstanceOf(AccessDeniedException.class);

        verify(questionRepository, never()).save(any());
    }

    @Test
    void answer_whenQuestionBelongsToDifferentInquiry_throwsResourceNotFoundException() {
        // Arrange — question exists but is tied to a different inquiry
        mockPrincipal("client-1", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));
        InquiryQuestion mismatch = InquiryQuestion.builder()
                .id("q-1").inquiryId("inquiry-OTHER").questionText("Pytanie?")
                .build();
        when(questionRepository.findById("q-1")).thenReturn(Optional.of(mismatch));

        // Act & Assert
        assertThatThrownBy(() -> questionService.answer("inquiry-1", "q-1", answerRequest("Odpowiedź")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("q-1");

        verify(questionRepository, never()).save(any());
    }
}
