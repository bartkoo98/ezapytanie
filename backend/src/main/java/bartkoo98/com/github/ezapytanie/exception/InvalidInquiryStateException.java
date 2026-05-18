package bartkoo98.com.github.ezapytanie.exception;

import bartkoo98.com.github.ezapytanie.enums.InquiryStatus;

public class InvalidInquiryStateException extends RuntimeException {

    public InvalidInquiryStateException(String inquiryId, InquiryStatus current, InquiryStatus required) {
        super("Inquiry " + inquiryId + " must be in " + required
                + " state for this operation, but is currently " + current);
    }

    public InvalidInquiryStateException(String message) {
        super(message);
    }
}
