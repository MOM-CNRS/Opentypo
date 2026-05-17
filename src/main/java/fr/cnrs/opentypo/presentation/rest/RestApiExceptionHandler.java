package fr.cnrs.opentypo.presentation.rest;

import fr.cnrs.opentypo.application.dto.api.ApiErrorMessages;
import fr.cnrs.opentypo.application.dto.api.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.stream.Collectors;

/**
 * JSON error responses for {@code /api/**} validation and HTTP status failures.
 */
@RestControllerAdvice(basePackages = "fr.cnrs.opentypo.presentation.rest")
public class RestApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + " : " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiErrorResponse(
                        HttpStatus.BAD_REQUEST.value(), ApiErrorMessages.VALIDATION_FAILED, details));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : ApiErrorMessages.httpStatusMessage(status.value());
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(status.value(), message, null));
    }
}
