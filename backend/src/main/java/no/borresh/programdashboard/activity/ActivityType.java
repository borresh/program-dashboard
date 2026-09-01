package no.borresh.programdashboard.activity;

/**
 * The vocabulary of the per-program activity log. Deliberately not constrained by a CHECK
 * constraint in the schema: unlike the role and status enums, this list is expected to
 * grow, and a database migration per new entry type would buy nothing.
 */
public enum ActivityType {
    PROGRAM_CREATED,
    PROGRAM_UPDATED,
    PROGRAM_STATUS_CHANGED,
    MILESTONE_ADDED,
    MILESTONE_UPDATED,
    MILESTONE_COMPLETED,
    MILESTONE_DELETED,
    CLARIFICATION_ASKED,
    CLARIFICATION_ANSWERED
}
