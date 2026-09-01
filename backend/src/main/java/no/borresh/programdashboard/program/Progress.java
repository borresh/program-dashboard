package no.borresh.programdashboard.program;

/**
 * R12. Progress is completed over total, and a program with no milestones has no progress
 * at all.
 *
 * <p>Reporting zero milestones as {@code 0%} would be a lie in the one direction that
 * matters: it makes a program nobody has broken down yet look like a program nobody has
 * started. {@code null} says "unknown", which is the truth.
 */
final class Progress {

    private Progress() {
    }

    static Double of(long completed, long total) {
        return total == 0 ? null : (double) completed / (double) total;
    }
}
