package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;
import bartkoo98.com.github.ezapytanie.model.Inquiry;
import bartkoo98.com.github.ezapytanie.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineSchedulerService {

    private final InquiryRepository inquiryRepository;
    private final AuditLogService auditLogService;

    @Scheduled(fixedDelay = 60_000)
    public void closeExpiredInquiries() {
        List<Inquiry> expired = inquiryRepository
                .findByStatusAndDeadlineBefore(InquiryStatus.PUBLISHED, Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        log.info("Closing {} expired inquiries", expired.size());

        for (Inquiry inquiry : expired) {
            inquiry.setStatus(InquiryStatus.CLOSED);
            inquiryRepository.save(inquiry);

            auditLogService.log(
                    "SYSTEM", "SYSTEM", "system",
                    "AUTO_CLOSED", "INQUIRY", inquiry.getId(),
                    Map.of("deadline", inquiry.getDeadline().toString())
            );
        }
    }
}
