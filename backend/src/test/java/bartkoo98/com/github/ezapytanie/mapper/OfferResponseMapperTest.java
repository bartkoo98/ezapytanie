package bartkoo98.com.github.ezapytanie.mapper;

import bartkoo98.com.github.ezapytanie.dto.response.OfferResponse;
import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.enums.OfferStatus;
import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.model.Offer;
import bartkoo98.com.github.ezapytanie.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class OfferResponseMapperTest {

    private OfferResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OfferResponseMapper();
    }

    private Offer offer() {
        return Offer.builder()
                .id("offer-1")
                .inquiryId("inquiry-1")
                .contractorId("contractor-1")
                .contractorName("Jan Kowalski")
                .price(new BigDecimal("5000.00"))
                .currency("PLN")
                .notes("Realizacja w 14 dni roboczych")
                .status(OfferStatus.SUBMITTED)
                .submittedAt(Instant.parse("2025-01-10T10:00:00Z"))
                .updatedAt(Instant.parse("2025-01-10T10:00:00Z"))
                .build();
    }

    private Inquiry inquiry(InquiryStatus status) {
        return Inquiry.builder()
                .id("inquiry-1")
                .title("Dostawa materiałów biurowych")
                .status(status)
                .build();
    }

    private User contractorUser() {
        return User.builder()
                .id("contractor-1")
                .institutionName("Firma Testowa Sp. z o.o.")
                .companyDetails(CompanyDetails.builder()
                        .nip("1234567890")
                        .regon("123456789")
                        .contactPhone("+48 111 222 333")
                        .contactEmail("kontakt@firma.pl")
                        .address("ul. Testowa 1, 00-001 Warszawa")
                        .build())
                .build();
    }

    @Test
    void toResponse_whenInquiryPublished_masksAllSensitiveFields() {
        OfferResponse result = mapper.toResponse(offer(), inquiry(InquiryStatus.PUBLISHED));

        assertThat(result.getPrice()).isNull();
        assertThat(result.getCurrency()).isNull();
        assertThat(result.getNotes()).isNull();
        assertThat(result.getContractorName()).isNull();
        assertThat(result.getContractorInstitutionName()).isNull();
        assertThat(result.getContractorCompanyDetails()).isNull();
    }

    @Test
    void toResponse_whenInquiryPublished_retainsNonSensitiveIdentifiers() {
        OfferResponse result = mapper.toResponse(offer(), inquiry(InquiryStatus.PUBLISHED));

        assertThat(result.getId()).isEqualTo("offer-1");
        assertThat(result.getInquiryId()).isEqualTo("inquiry-1");
        assertThat(result.getInquiryTitle()).isEqualTo("Dostawa materiałów biurowych");
        assertThat(result.getContractorId()).isEqualTo("contractor-1");
        assertThat(result.getStatus()).isEqualTo(OfferStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    void toResponse_whenInquiryClosed_revealsAllSensitiveFields() {
        OfferResponse result = mapper.toResponse(offer(), inquiry(InquiryStatus.CLOSED));

        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getCurrency()).isEqualTo("PLN");
        assertThat(result.getNotes()).isEqualTo("Realizacja w 14 dni roboczych");
        assertThat(result.getContractorName()).isEqualTo("Jan Kowalski");
    }

    @Test
    void toResponse_whenInquiryArchived_revealsAllSensitiveFields() {
        OfferResponse result = mapper.toResponse(offer(), inquiry(InquiryStatus.ARCHIVED));

        assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(result.getContractorName()).isEqualTo("Jan Kowalski");
    }

    @Test
    void toResponse_whenRevealedWithContractor_exposesFullCompanyProfile() {
        OfferResponse result = mapper.toResponse(offer(), true, "Dostawa materiałów biurowych", contractorUser());

        assertThat(result.getContractorInstitutionName()).isEqualTo("Firma Testowa Sp. z o.o.");
        assertThat(result.getContractorCompanyDetails()).isNotNull();
        assertThat(result.getContractorCompanyDetails().getNip()).isEqualTo("1234567890");
        assertThat(result.getContractorCompanyDetails().getContactEmail()).isEqualTo("kontakt@firma.pl");
        assertThat(result.getContractorCompanyDetails().getAddress()).isEqualTo("ul. Testowa 1, 00-001 Warszawa");
    }

    @Test
    void toResponse_whenSealedWithContractor_masksCompanyProfileAndPrice() {
        OfferResponse result = mapper.toResponse(offer(), false, "Dostawa materiałów biurowych", contractorUser());

        assertThat(result.getContractorInstitutionName()).isNull();
        assertThat(result.getContractorCompanyDetails()).isNull();
        assertThat(result.getPrice()).isNull();
        assertThat(result.getContractorName()).isNull();
    }

    @Test
    void toResponse_whenRevealedWithNullContractor_returnsNullCompanyFields() {
        OfferResponse result = mapper.toResponse(offer(), true, "Tytuł", null);

        assertThat(result.getContractorInstitutionName()).isNull();
        assertThat(result.getContractorCompanyDetails()).isNull();
        assertThat(result.getContractorName()).isEqualTo("Jan Kowalski");
        assertThat(result.getPrice()).isEqualByComparingTo("5000.00");
    }

    @Test
    void toMaskedCompetitorResponse_nullsContractorIdentity() {
        OfferResponse result = mapper.toMaskedCompetitorResponse(offer(), "Dostawa materiałów biurowych");

        assertThat(result.getContractorId()).isNull();
        assertThat(result.getContractorName()).isNull();
        assertThat(result.getContractorCompanyDetails()).isNull();
        assertThat(result.getPrice()).isNull();
        assertThat(result.getCurrency()).isNull();
    }

    @Test
    void toMaskedCompetitorResponse_setsAnonymousPlaceholders() {
        OfferResponse result = mapper.toMaskedCompetitorResponse(offer(), "Dostawa materiałów biurowych");

        assertThat(result.getContractorInstitutionName()).isEqualTo("Anonimowy Wykonawca");
        assertThat(result.getNotes()).isEqualTo("Zapieczętowane");
    }

    @Test
    void toMaskedCompetitorResponse_retainsNonIdentifyingMetadata() {
        OfferResponse result = mapper.toMaskedCompetitorResponse(offer(), "Dostawa materiałów biurowych");

        assertThat(result.getId()).isEqualTo("offer-1");
        assertThat(result.getInquiryId()).isEqualTo("inquiry-1");
        assertThat(result.getInquiryTitle()).isEqualTo("Dostawa materiałów biurowych");
        assertThat(result.getStatus()).isEqualTo(OfferStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
    }
}
