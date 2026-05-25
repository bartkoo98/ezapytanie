package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.SubmitOfferRequest;
import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.enums.UserRole;
import bartkoo98.com.github.ezapytanie.exception.DuplicateOfferException;
import bartkoo98.com.github.ezapytanie.exception.InquiryNotOpenException;
import bartkoo98.com.github.ezapytanie.mapper.OfferResponseMapper;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import bartkoo98.com.github.ezapytanie.repository.OfferRepository;
import bartkoo98.com.github.ezapytanie.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for sealed-bid submission rules and role-based offer visibility.
 */
@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

    @Mock private OfferRepository offerRepository;
    @Mock private InquiryRepository inquiryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private OfferResponseMapper mapper;

    @InjectMocks
    private OfferService offerService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void mockPrincipal(String userId, String fullName, UserRole role) {
        User user = User.builder()
                .id(userId).fullName(fullName).role(role).email(userId + "@test.pl")
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private Inquiry publishedInquiry(String id, String clientId) {
        return Inquiry.builder().id(id).title("Dostawa materiałów")
                .status(InquiryStatus.PUBLISHED).clientId(clientId).build();
    }

    private Offer submittedOffer(String offerId, String inquiryId, String contractorId) {
        return Offer.builder()
                .id(offerId).inquiryId(inquiryId).contractorId(contractorId)
                .contractorName("Jan Kowalski").price(new BigDecimal("5000.00"))
                .currency("PLN").status(OfferStatus.SUBMITTED).build();
    }

    private SubmitOfferRequest submitRequest(String inquiryId) {
        SubmitOfferRequest req = new SubmitOfferRequest();
        req.setInquiryId(inquiryId);
        req.setPrice(new BigDecimal("5000.00"));
        req.setNotes("Realizacja w 14 dni");
        return req;
    }

    @Test
    void submit_whenInquiryPublishedAndNoDuplicate_savesOfferWithCorrectState() {
        // Arrange
        mockPrincipal("contractor-1", "Jan Kowalski", UserRole.CONTRACTOR);
        Inquiry inquiry = publishedInquiry("inquiry-1", "client-1");
        Offer saved = submittedOffer("offer-1", "inquiry-1", "contractor-1");
        OfferResponse expectedResponse = OfferResponse.builder().id("offer-1").build();

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));
        when(offerRepository.existsByInquiryIdAndContractorId("inquiry-1", "contractor-1")).thenReturn(false);
        when(offerRepository.save(any())).thenReturn(saved);
        when(mapper.toResponse(eq(saved), eq(inquiry))).thenReturn(expectedResponse);

        // Act
        OfferResponse result = offerService.submit(submitRequest("inquiry-1"), "127.0.0.1", "JUnit");

        // Assert
        assertThat(result.getId()).isEqualTo("offer-1");

        ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);
        verify(offerRepository).save(captor.capture());
        Offer persisted = captor.getValue();
        assertThat(persisted.getContractorId()).isEqualTo("contractor-1");
        assertThat(persisted.getContractorName()).isEqualTo("Jan Kowalski");
        assertThat(persisted.getStatus()).isEqualTo(OfferStatus.SUBMITTED);
    }

    @Test
    void submit_whenInquiryNotPublished_throwsInquiryNotOpenException() {
        // Arrange — principal not needed: exception thrown before getCurrentPrincipal()
        Inquiry closed = Inquiry.builder().id("inquiry-1").status(InquiryStatus.CLOSED).clientId("client-1").build();
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(closed));

        // Act & Assert
        assertThatThrownBy(() -> offerService.submit(submitRequest("inquiry-1"), "127.0.0.1", "JUnit"))
                .isInstanceOf(InquiryNotOpenException.class)
                .hasMessageContaining("inquiry-1");

        verify(offerRepository, never()).save(any());
    }

    @Test
    void submit_whenDuplicateOffer_throwsDuplicateOfferException() {
        // Arrange
        mockPrincipal("contractor-1", "Jan Kowalski", UserRole.CONTRACTOR);
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));
        when(offerRepository.existsByInquiryIdAndContractorId("inquiry-1", "contractor-1")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> offerService.submit(submitRequest("inquiry-1"), "127.0.0.1", "JUnit"))
                .isInstanceOf(DuplicateOfferException.class)
                .hasMessageContaining("inquiry-1");

        verify(offerRepository, never()).save(any());
    }

    @Test
    void listForInquiry_asClientOwner_whenPublished_requestsMaskedMapping() {
        // Arrange — CLIENT is the owner, inquiry is still PUBLISHED (sealed)
        mockPrincipal("client-1", "Anna Nowak", UserRole.CLIENT);
        Inquiry inquiry = publishedInquiry("inquiry-1", "client-1");
        Offer offer = submittedOffer("offer-1", "inquiry-1", "contractor-1");
        OfferResponse masked = OfferResponse.builder().id("offer-1").price(null).build();

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(offer));
        when(mapper.toResponse(eq(offer), eq(false), eq("Dostawa materiałów"), isNull())).thenReturn(masked);

        // Act
        List<OfferResponse> result = offerService.listForInquiry("inquiry-1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isNull();
        verify(userRepository, never()).findAllById(any()); // no contractor lookup while sealed
    }

    @Test
    void listForInquiry_asClientOwner_whenClosed_revealsOffersAndLooksUpContractors() {
        // Arrange — inquiry is CLOSED, bids are now revealed
        mockPrincipal("client-1", "Anna Nowak", UserRole.CLIENT);
        Inquiry closed = Inquiry.builder().id("inquiry-1").title("Dostawa materiałów")
                .status(InquiryStatus.CLOSED).clientId("client-1").build();
        Offer offer = submittedOffer("offer-1", "inquiry-1", "contractor-1");
        User contractor = User.builder().id("contractor-1").institutionName("Firma X Sp. z o.o.").build();
        OfferResponse revealed = OfferResponse.builder().id("offer-1").price(new BigDecimal("5000.00")).build();

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(closed));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(offer));
        when(userRepository.findAllById(any())).thenReturn(List.of(contractor));
        when(mapper.toResponse(eq(offer), eq(true), eq("Dostawa materiałów"), eq(contractor))).thenReturn(revealed);

        // Act
        List<OfferResponse> result = offerService.listForInquiry("inquiry-1");

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("5000.00");
        verify(userRepository).findAllById(any()); // contractor profile fetched
    }

    @Test
    void listForInquiry_asClientNonOwner_throwsAccessDeniedException() {
        // Arrange — different client tries to read another client's inquiry offers
        mockPrincipal("other-client", "Ktoś Inny", UserRole.CLIENT);
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(publishedInquiry("inquiry-1", "client-1")));

        // Act & Assert
        assertThatThrownBy(() -> offerService.listForInquiry("inquiry-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void listForInquiry_asContractor_routesOwnOfferToRevealAndCompetitorsToMask() {
        // Arrange — two offers: one belongs to the caller, one to a competitor
        mockPrincipal("contractor-1", "Jan Kowalski", UserRole.CONTRACTOR);
        Inquiry inquiry = publishedInquiry("inquiry-1", "client-1");
        Offer myOffer = submittedOffer("offer-1", "inquiry-1", "contractor-1");
        Offer competitorOffer = submittedOffer("offer-2", "inquiry-1", "contractor-2");
        OfferResponse ownRevealed = OfferResponse.builder().id("offer-1").price(new BigDecimal("5000.00")).build();
        OfferResponse competitorMasked = OfferResponse.builder().id("offer-2").contractorId(null).price(null).build();

        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));
        when(offerRepository.findByInquiryId("inquiry-1")).thenReturn(List.of(myOffer, competitorOffer));
        when(mapper.toResponse(eq(myOffer), eq(true), eq("Dostawa materiałów"))).thenReturn(ownRevealed);
        when(mapper.toMaskedCompetitorResponse(eq(competitorOffer), eq("Dostawa materiałów"))).thenReturn(competitorMasked);

        // Act
        List<OfferResponse> result = offerService.listForInquiry("inquiry-1");

        // Assert — both offers returned (competitor is visible but masked)
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPrice()).isEqualByComparingTo("5000.00"); // own offer revealed
        assertThat(result.get(1).getContractorId()).isNull();                  // competitor identity hidden
        assertThat(result.get(1).getPrice()).isNull();                          // competitor price hidden

        verify(mapper).toResponse(eq(myOffer), eq(true), eq("Dostawa materiałów"));
        verify(mapper).toMaskedCompetitorResponse(eq(competitorOffer), eq("Dostawa materiałów"));
    }
}
