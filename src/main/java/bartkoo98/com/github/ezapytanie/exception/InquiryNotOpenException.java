package bartkoo98.com.github.ezapytanie.exception;

public class InquiryNotOpenException extends RuntimeException {

    public InquiryNotOpenException(String inquiryId) {
        super("Inquiry " + inquiryId + " is not open for offer submissions");
    }
}
