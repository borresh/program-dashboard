package no.borresh.programdashboard.agent;

/**
 * What an agent does. {@code REVIEWER} is accepted and stored even though nothing
 * consumes it in v1: an agent describing itself as a reviewer is meaningful information
 * today, and review handoff arrives in v1.1.
 */
public enum AgentRole {
    IMPLEMENTER,
    REVIEWER,
    HUMAN
}
