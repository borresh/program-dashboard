package no.borresh.programdashboard.common;

/**
 * Thrown when a request is well-formed but illegal in the current state: a duplicate
 * slug, a second answer to an answered clarification, completing a completed milestone.
 * Rendered as a 409 Problem Details response by {@link ApiExceptionHandler}.
 *
 * <p>The detail is expected to explain the current state and who put it there, so the
 * caller does not have to issue a second request to find out.
 */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String title;

    public ConflictException(String title, String detail) {
        super(detail);
        this.title = title;
    }

    public String title() {
        return title;
    }
}
