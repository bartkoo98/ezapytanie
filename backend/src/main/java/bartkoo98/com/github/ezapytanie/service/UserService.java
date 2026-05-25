package bartkoo98.com.github.ezapytanie.service;

import bartkoo98.com.github.ezapytanie.dto.request.UpdateProfileRequest;
import bartkoo98.com.github.ezapytanie.dto.response.UserResponse;
import bartkoo98.com.github.ezapytanie.exception.ResourceNotFoundException;
import bartkoo98.com.github.ezapytanie.model.CompanyDetails;
import bartkoo98.com.github.ezapytanie.model.User;
import bartkoo98.com.github.ezapytanie.repository.UserRepository;
import bartkoo98.com.github.ezapytanie.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse getProfile() {
        User user = currentUser();
        return toUserResponse(user);
    }

    public UserResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();

        user.setFullName(request.getFullName());

        CompanyDetails cd = user.getCompanyDetails() != null
                ? user.getCompanyDetails()
                : CompanyDetails.builder().build();
        cd.setContactPhone(request.getContactPhone());
        cd.setAddress(request.getAddress());
        user.setCompanyDetails(cd);

        return toUserResponse(userRepository.save(user));
    }

    private User currentUser() {
        CustomUserDetails principal = (CustomUserDetails)
                SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUserId()));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .institutionName(user.getInstitutionName())
                .companyDetails(user.getCompanyDetails())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
