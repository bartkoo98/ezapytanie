package bartkoo98.com.github.ezapytanie.repository;

import bartkoo98.com.github.ezapytanie.model.AuditLog;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

// Extends bare Repository (not CrudRepository/MongoRepository) to enforce append-only access.
// No delete or update methods are available — by design (NFR-AUD-01).
public interface AuditLogRepository extends Repository<AuditLog, String> {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEntityId(String entityId);

    List<AuditLog> findByActorId(String actorId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByTimestampBetween(Instant from, Instant to);
}
