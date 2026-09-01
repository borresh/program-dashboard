package no.borresh.programdashboard.agent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "agent")
public class Agent {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentRole role;

    private String description;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    protected Agent() {
        // for JPA
    }

    public Agent(UUID id, String name, AgentRole role, String description, Instant registeredAt) {
        this.id = id;
        this.name = name;
        this.role = role;
        this.description = description;
        this.registeredAt = registeredAt;
        this.lastSeenAt = registeredAt;
    }

    /** Records that this agent was heard from. See R3. */
    public void markSeenAt(Instant seenAt) {
        this.lastSeenAt = seenAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public AgentRole getRole() {
        return role;
    }

    public String getDescription() {
        return description;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }
}
