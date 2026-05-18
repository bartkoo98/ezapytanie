package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.model.AuditLog;
import bartkoo98.com.github.ezapytanie.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(String actorId,
                    String actorRole,
                    String actorEmail,
                    String action,
                    String entityType,
                    String entityId,
                    Map<String, Object> details,
                    String ipAddress,
                    String userAgent) {

        AuditLog entry = AuditLog.builder()
                .timestamp(Instant.now())
                .actorId(actorId)
                .actorRole(actorRole)
                .actorEmail(actorEmail)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        auditLogRepository.save(entry);
    }
}
