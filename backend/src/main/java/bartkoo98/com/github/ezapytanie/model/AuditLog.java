package bartkoo98.com.github.ezapytanie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private Instant timestamp;

    @Indexed
    private String actorId;     // ObjectId or "SYSTEM"
    private String actorRole;
    private String actorEmail;

    @Indexed
    private String action;      // e.g. USER_REGISTERED, INQUIRY_CREATED, OFFER_SUBMITTED

    private String entityType;  // USER, INQUIRY, OFFER, SYSTEM

    @Indexed
    private String entityId;

    private Map<String, Object> details;
    private String ipAddress;
    private String userAgent;
}
