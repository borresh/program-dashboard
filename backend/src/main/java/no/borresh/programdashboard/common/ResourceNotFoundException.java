package no.borresh.programdashboard.common;

/**
 * Thrown when an identifier in a request does not resolve. Rendered as a 404 Problem
 * Details response by {@link ApiExceptionHandler}.
 *
 * <p>The detail is expected to name the identifier that failed to resolve, so the caller
 * can tell which of several identifiers in a request was wrong.
 */
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String title;

    public ResourceNotFoundException(String title, String detail) {
        super(detail);
        this.title = title;
    }

    public String title() {
        return title;
    }
}
