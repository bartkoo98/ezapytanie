package bartkoo98.com.github.ezapytanie.exception;

public class DuplicateOfferException extends RuntimeException {

    public DuplicateOfferException(String inquiryId) {
        super("You have already submitted an offer for inquiry: " + inquiryId);
    }
}
