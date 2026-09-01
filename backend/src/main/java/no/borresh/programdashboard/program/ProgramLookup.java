package no.borresh.programdashboard.program;

import java.util.Optional;
import java.util.UUID;
import no.borresh.programdashboard.common.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * R8. Resolves the {@code {idOrSlug}} path variable that every nested route carries.
 *
 * <p>Its own service rather than a method on {@link ProgramService} because milestones,
 * clarifications and programs all need it. Folding it into {@code ProgramService} would
 * make the clarification service depend on the program service, which in turn has to read
 * clarifications for the detail response — a cycle Spring refuses to construct.
 */
@Service
public class ProgramLookup {

    private final ProgramRepository programs;

    ProgramLookup(ProgramRepository programs) {
        this.programs = programs;
    }

    @Transactional(readOnly = true)
    public Program require(String idOrSlug) {
        return asUuid(idOrSlug)
                .flatMap(programs::findById)
                .or(() -> programs.findBySlug(idOrSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Program not found",
                        "No program exists with id or slug '%s'.".formatted(idOrSlug)));
    }

    private static Optional<UUID> asUuid(String candidate) {
        try {
            return Optional.of(UUID.fromString(candidate));
        } catch (IllegalArgumentException notAUuid) {
            // Expected: the same path variable accepts a slug, so a non-UUID is routine
            // rather than exceptional. Fall through to the slug lookup.
            return Optional.empty();
        }
    }
}
