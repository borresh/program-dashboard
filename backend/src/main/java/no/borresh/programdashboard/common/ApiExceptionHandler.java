package no.borresh.programdashboard.common;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import tools.jackson.databind.exc.InvalidFormatException;

/**
 * The single place that turns an exception into a response body. Every error the API emits
 * is RFC 7807 Problem Details and passes through here; controllers never build an error
 * body themselves.
 *
 * <p>Extending {@link ResponseEntityExceptionHandler} means Spring's own failures — wrong
 * method, unsupported media type, unknown route — already arrive as Problem Details with
 * the right status. Without it a catch-all would flatten every one of them to 500.
 *
 * <p>Two rules hold for every handler below. The {@code detail} names the offending input
 * and what was expected, because "Invalid request" tells a caller nothing. And no stack
 * trace ever reaches a response body: unexpected failures are logged in full server-side
 * and reduced to a single sentence for the caller.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    /** One invalid field, and what the API expected instead. */
    record FieldProblem(String field, String expected, String rejectedValue) {}

    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException exception) {
        LOG.warn("404 {}: {}", exception.title(), exception.getMessage());
        return respond(HttpStatus.NOT_FOUND, exception.title(), exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    ResponseEntity<ProblemDetail> handleConflict(ConflictException exception) {
        // Every rejected state transition is worth a WARN: these are the errors that mean
        // two actors disagree about the state of a program.
        LOG.warn("409 {}: {}", exception.title(), exception.getMessage());
        return respond(HttpStatus.CONFLICT, exception.title(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        String expected = exception.getRequiredType() == null
                ? "a different type"
                : exception.getRequiredType().getSimpleName();
        String detail = "Parameter '%s' has value '%s', which is not a valid %s."
                .formatted(exception.getName(), exception.getValue(), expected);

        LOG.warn("400 parameter type mismatch: {}", detail);
        return respond(HttpStatus.BAD_REQUEST, "Invalid parameter", detail);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        LOG.error("Unhandled exception; returning 500", exception);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                "The request failed for an unexpected reason. The cause has been logged by the server.");
    }

    /** R6. Every offending field is listed, with what it expected, not just the first one. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        List<FieldProblem> errors = collectFieldProblems(exception);
        String fields = errors.stream().map(FieldProblem::field).distinct()
                .reduce((a, b) -> a + ", " + b).orElse("(none)");

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid request body",
                "Request has %d invalid field(s): %s. See the 'errors' property for what each one expects."
                        .formatted(errors.size(), fields));
        problem.setProperty("errors", errors);

        LOG.warn("400 invalid request body: {}", fields);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        String detail = exception.getCause() instanceof InvalidFormatException invalidFormat
                ? describeInvalidFormat(invalidFormat)
                : "Request body could not be parsed as JSON. Expected a JSON object.";

        LOG.warn("400 unreadable request body: {}", detail);
        return ResponseEntity.badRequest().body(problem(HttpStatus.BAD_REQUEST, "Malformed request body", detail));
    }

    private static List<FieldProblem> collectFieldProblems(MethodArgumentNotValidException exception) {
        Stream<FieldProblem> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::toFieldProblem);
        Stream<FieldProblem> objects = exception.getBindingResult().getGlobalErrors().stream()
                .map(ApiExceptionHandler::toFieldProblem);

        return Stream.concat(fields, objects)
                .sorted(Comparator.comparing(FieldProblem::field))
                .toList();
    }

    private static FieldProblem toFieldProblem(FieldError error) {
        Object rejected = error.getRejectedValue();
        return new FieldProblem(error.getField(), error.getDefaultMessage(),
                rejected == null ? null : String.valueOf(rejected));
    }

    private static FieldProblem toFieldProblem(ObjectError error) {
        return new FieldProblem(error.getObjectName(), error.getDefaultMessage(), null);
    }

    private static String describeInvalidFormat(InvalidFormatException exception) {
        String field = exception.getPath().stream()
                .map(reference -> reference.getPropertyName() == null ? "[]" : reference.getPropertyName())
                .reduce((a, b) -> a + "." + b)
                .orElse("(root)");

        Class<?> targetType = exception.getTargetType();
        String expected = targetType != null && targetType.isEnum()
                ? "one of " + String.join(", ", enumNames(targetType))
                : "a value of type " + (targetType == null ? "unknown" : targetType.getSimpleName());

        return "Field '%s' has value '%s', but must be %s.".formatted(field, exception.getValue(), expected);
    }

    private static List<String> enumNames(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(Object::toString).toList();
    }

    private static ResponseEntity<ProblemDetail> respond(HttpStatus status, String title, String detail) {
        return ResponseEntity.status(status).body(problem(status, title, detail));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
