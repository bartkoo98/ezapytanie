package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.CancelInquiryRequest;
import bartkoo98.com.github.ezapytanie.dto.response.InquiryResponse;
import bartkoo98.com.github.ezapytanie.service.AuditLogService;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.exception.InvalidInquiryStateException;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.repository.OfferRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock private InquiryRepository inquiryRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private InquiryService inquiryService;

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

    private Inquiry inquiry(String id, String clientId, InquiryStatus status) {
        return Inquiry.builder()
                .id(id).title("Dostawa materiałów").clientId(clientId).status(status)
                .build();
    }

    private Offer offer(String id, String inquiryId, OfferStatus status) {
        return Offer.builder()
                .id(id).inquiryId(inquiryId).contractorId("contractor-1")
                .price(new BigDecimal("5000.00")).currency("PLN").status(status)
                .build();
    }

    private CancelInquiryRequest cancelRequest() {
        CancelInquiryRequest req = new CancelInquiryRequest();
        req.setCancellationReason("Zmiana planów zakupowych");
        return req;
    }

    @Test
    void cancel_asOwner_whenPublished_changesStatusAndRejectsSubmittedOffers() {
        mockPrincipal("client-1", UserRole.CLIENT);
        Inquiry published = inquiry("inquiry-1", "client-1", InquiryStatus.PUBLISHED);
        Offer submitted = offer("offer-1", "inquiry-1", OfferStatus.SUBMITTED);
        Offer withdrawn = offer("offer-2", "inquiry-1", OfferStatus.WITHDRAWN);

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(published));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(submitted, withdrawn));
        when(inquiryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InquiryResponse result = inquiryService.cancel("inquiry-1", cancelRequest());

        assertThat(result.getStatus()).isEqualTo(InquiryStatus.CANCELLED);
        assertThat(result.getCancellationReason()).isEqualTo("Zmiana planów zakupowych");

        ArgumentCaptor<List<Offer>> savedOffers = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(savedOffers.capture());
        List<Offer> rejected = savedOffers.getValue();
        assertThat(rejected).hasSize(1);
        assertThat(rejected.get(0).getId()).isEqualTo("offer-1");
        assertThat(rejected.get(0).getStatus()).isEqualTo(OfferStatus.REJECTED);
    }

    @Test
    void cancel_onlyRejectsSubmittedOffers_notAlreadyWithdrawn() {
        mockPrincipal("client-1", UserRole.CLIENT);
        Inquiry published = inquiry("inquiry-1", "client-1", InquiryStatus.PUBLISHED);
        Offer withdrawn = offer("offer-w", "inquiry-1", OfferStatus.WITHDRAWN);

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(published));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(withdrawn));
        when(inquiryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        inquiryService.cancel("inquiry-1", cancelRequest());

        ArgumentCaptor<List<Offer>> savedOffers = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(savedOffers.capture());
        assertThat(savedOffers.getValue()).isEmpty();
        assertThat(withdrawn.getStatus()).isEqualTo(OfferStatus.WITHDRAWN);
    }

    @Test
    void cancel_asNonOwner_throwsAccessDeniedException() {
        mockPrincipal("other-client", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(inquiry("inquiry-1", "client-1", InquiryStatus.PUBLISHED)));

        assertThatThrownBy(() -> inquiryService.cancel("inquiry-1", cancelRequest()))
                .isInstanceOf(AccessDeniedException.class);

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void cancel_asOwner_whenNotPublished_throwsInvalidInquiryStateException() {
        mockPrincipal("client-1", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(inquiry("inquiry-1", "client-1", InquiryStatus.CLOSED)));

        assertThatThrownBy(() -> inquiryService.cancel("inquiry-1", cancelRequest()))
                .isInstanceOf(InvalidInquiryStateException.class)
                .hasMessageContaining("inquiry-1");

        verify(inquiryRepository, never()).save(any());
    }

    @Test
    void acceptOffer_asOwner_whenClosed_archivesInquiryAndSelectsWinner() {
        mockPrincipal("client-1", UserRole.CLIENT);
        Inquiry closed = inquiry("inquiry-1", "client-1", InquiryStatus.CLOSED);
        Offer winner = offer("offer-1", "inquiry-1", OfferStatus.SUBMITTED);
        Offer loser = offer("offer-2", "inquiry-1", OfferStatus.SUBMITTED);

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(closed));
        when(offerRepository.findById("offer-1")).thenReturn(Optional.of(winner));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(winner, loser));
        when(inquiryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InquiryResponse result = inquiryService.acceptOffer("inquiry-1", "offer-1", "Najlepsza cena");

        assertThat(result.getStatus()).isEqualTo(InquiryStatus.ARCHIVED);
        assertThat(result.getWinnerOfferId()).isEqualTo("offer-1");
        assertThat(result.getSelectionJustification()).isEqualTo("Najlepsza cena");
        assertThat(winner.getStatus()).isEqualTo(OfferStatus.SELECTED);

        ArgumentCaptor<List<Offer>> savedOthers = ArgumentCaptor.forClass(List.class);
        verify(offerRepository).saveAll(savedOthers.capture());
        assertThat(savedOthers.getValue()).hasSize(1);
        assertThat(savedOthers.getValue().get(0).getStatus()).isEqualTo(OfferStatus.REJECTED);
    }

    @Test
    void acceptOffer_asOwner_whenNotClosed_throwsInvalidInquiryStateException() {
        mockPrincipal("client-1", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(inquiry("inquiry-1", "client-1", InquiryStatus.PUBLISHED)));

        assertThatThrownBy(() -> inquiryService.acceptOffer("inquiry-1", "offer-1", "Uzasadnienie"))
                .isInstanceOf(InvalidInquiryStateException.class)
                .hasMessageContaining("inquiry-1");

        verify(offerRepository, never()).save(any());
    }

    @Test
    void getById_asContractor_whenCancelled_throwsAccessDeniedException() {
        mockPrincipal("contractor-1", UserRole.CONTRACTOR);
        when(inquiryRepository.findById("inquiry-1"))
                .thenReturn(Optional.of(inquiry("inquiry-1", "client-1", InquiryStatus.CANCELLED)));

        assertThatThrownBy(() -> inquiryService.getById("inquiry-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getById_asContractor_whenArchived_masksSelectionJustification() {
        mockPrincipal("contractor-1", UserRole.CONTRACTOR);
        Inquiry archived = Inquiry.builder()
                .id("inquiry-1").title("Dostawa").clientId("client-1")
                .status(InquiryStatus.ARCHIVED)
                .selectionJustification("Wygrała firma X bo miała niższą cenę")
                .winnerOfferId("offer-1")
                .build();
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(archived));

        InquiryResponse result = inquiryService.getById("inquiry-1");

        assertThat(result.getSelectionJustification()).isNull();
        assertThat(result.getWinnerOfferId()).isEqualTo("offer-1");
    }
}
