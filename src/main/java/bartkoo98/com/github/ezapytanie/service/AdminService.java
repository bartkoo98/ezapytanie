package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.response.UserResponse;
import bartkoo98.com.github.ezapytanie.model.AuditLog;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toUserResponse)
                .toList();
    }

    public List<AuditLog> listAuditLogs(String entityId, String actorId,
                                         String action, Instant from, Instant to) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (entityId != null) criteriaList.add(Criteria.where("entityId").is(entityId));
        if (actorId != null)  criteriaList.add(Criteria.where("actorId").is(actorId));
        if (action != null)   criteriaList.add(Criteria.where("action").is(action));

        if (from != null || to != null) {
            Criteria timeCriteria = Criteria.where("timestamp");
            if (from != null) timeCriteria = timeCriteria.gte(from);
            if (to != null)   timeCriteria = timeCriteria.lte(to);
            criteriaList.add(timeCriteria);
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        query.with(Sort.by(Sort.Direction.DESC, "timestamp"));

        return mongoTemplate.find(query, AuditLog.class);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .institutionName(user.getInstitutionName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
