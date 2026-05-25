package bartkoo98.com.github.ezapytanie.exception;

import bartkoo98.com.github.ezapytanie.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(new ErrorResponse("NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InquiryNotOpenException.class)
    public ResponseEntity<ErrorResponse> handleInquiryNotOpen(InquiryNotOpenException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("INQUIRY_NOT_OPEN", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateOfferException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateOffer(DuplicateOfferException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("DUPLICATE_OFFER", ex.getMessage()));
    }

    @ExceptionHandler(InvalidInquiryStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidState(InvalidInquiryStateException ex) {
        return ResponseEntity.status(409).body(new ErrorResponse("INVALID_STATE", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(403).body(new ErrorResponse("FORBIDDEN", "Access denied"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(500).body(new ErrorResponse("INTERNAL_ERROR", "An unexpected error occurred"));
    }
}
