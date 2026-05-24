package bartkoo98.com.github.ezapytanie.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDetails {
    private String nip;
    private String regon;
    private String contactPhone;
    private String contactEmail;
    private String address;
}
