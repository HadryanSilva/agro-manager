package br.com.hadryan.agro.manager.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;

/**
 * Central translator from exceptions to HTTP responses.
 *
 * <p>Produces RFC 7807 {@link ProblemDetail} bodies so clients see a
 * consistent error shape regardless of which layer threw. Every response
 * includes a {@code timestamp} extension property to aid log correlation.
 *
 * <p>Ordering matters: more specific handlers appear first so they win over
 * fallbacks. The final {@code Exception} handler exists so unexpected errors
 * never leak a stack trace to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------------------------------------------------------------------
    // Application exceptions — predictable mappings to HTTP status codes.
    // ---------------------------------------------------------------------

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex, HttpServletRequest request) {
        log.debug("Resource not found: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusiness(BusinessException ex, HttpServletRequest request) {
        log.debug("Business rule violated: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Business Rule Violated",
                ex.getMessage(), request);
    }

    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest request) {
        log.debug("Conflict: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    // ---------------------------------------------------------------------
    // Validation — surface field-level details so the UI can highlight them.
    // ---------------------------------------------------------------------

    /**
     * Triggered when {@code @Valid} on a request body fails. The {@code
     * fieldErrors} extension property carries one entry per invalid field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleBodyValidation(MethodArgumentNotValidException ex,
                                              HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldErrorDetail::from)
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more fields are invalid", request);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    /**
     * Triggered when {@code @Validated} on a path/query parameter fails.
     * The structure mirrors body validation for client convenience.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleParamValidation(ConstraintViolationException ex,
                                               HttpServletRequest request) {
        List<FieldErrorDetail> fieldErrors = ex.getConstraintViolations().stream()
                .map(FieldErrorDetail::from)
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more parameters are invalid", request);
        problem.setProperty("fieldErrors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex,
                                              HttpServletRequest request) {
        log.debug("Unreadable request body: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed Request",
                "Request body is missing or malformed", request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                            HttpServletRequest request) {
        String detail = "Parameter '%s' has an invalid value".formatted(ex.getName());
        return problem(HttpStatus.BAD_REQUEST, "Invalid Parameter", detail, request);
    }

    // ---------------------------------------------------------------------
    // Persistence — translate technical errors into user-facing statuses.
    // ---------------------------------------------------------------------

    /**
     * Typically a unique-constraint or foreign-key violation. We intentionally
     * don't echo the DB message — it usually leaks table/column names. The
     * service layer should catch this upstream and throw {@code
     * ConflictException} with a proper message; this is the safety net.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex,
                                             HttpServletRequest request) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT, "Data Integrity Violation",
                "The request could not be completed due to a data conflict", request);
    }

    /**
     * Another request updated the same row first. Client should re-fetch
     * and retry with the new version.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        log.debug("Optimistic locking failure: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, "Concurrent Modification",
                "The resource was modified by another request. Please reload and try again.",
                request);
    }

    // ---------------------------------------------------------------------
    // 404 for unmapped routes — Spring Boot throws this when no handler
    // matches (with spring.mvc.throw-exception-if-no-handler-found=true or
    // on Boot 3.2+ by default when static resources are enabled).
    // ---------------------------------------------------------------------

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResource(NoResourceFoundException ex,
                                          HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Endpoint Not Found",
                "No endpoint matches %s %s".formatted(request.getMethod(), request.getRequestURI()),
                request);
    }

    // ---------------------------------------------------------------------
    // Fallback — anything we didn't explicitly handle. Log at ERROR, hide
    // details from the client.
    // ---------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.", request);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private static ProblemDetail problem(HttpStatus status, String title, String detail,
                                         HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    /**
     * Minimal shape for field-level validation errors. Kept as a record for
     * easy JSON serialization.
     */
    public record FieldErrorDetail(String field, String message) {

        static FieldErrorDetail from(FieldError error) {
            return new FieldErrorDetail(error.getField(), error.getDefaultMessage());
        }

        static FieldErrorDetail from(ConstraintViolation<?> violation) {
            // For param validation the "field" is something like
            // "methodName.paramName"; we keep only the paramName for clarity.
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            return new FieldErrorDetail(field, violation.getMessage());
        }
    }
}