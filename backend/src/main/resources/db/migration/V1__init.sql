-- Program Dashboard initial schema.
--
-- This migration runs unchanged on PostgreSQL 16 and on H2 in PostgreSQL mode,
-- which is what keeps repository tests off Testcontainers. That constrains it:
-- no JSONB, no arrays, no ON CONFLICT, no gen_random_uuid(). UUIDs are generated
-- in Java. Enums are varchar plus a CHECK constraint rather than a PostgreSQL
-- ENUM type, for the same reason.
--
-- Column names avoid `position` and `type`; both are function names in HQL and
-- in at least one of the two engines. The API field names are unaffected because
-- request and response records are mapped explicitly from entities.
--
-- All timestamps are stored as `timestamp with time zone` and written from Java
-- `Instant`, so they are UTC regardless of the server's time zone.

CREATE TABLE agent
(
    id            uuid                        NOT NULL,
    name          varchar(100)                NOT NULL,
    role          varchar(20)                 NOT NULL,
    description   varchar(500),
    registered_at timestamp(6) with time zone NOT NULL,
    last_seen_at  timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_agent PRIMARY KEY (id),
    CONSTRAINT uq_agent_name UNIQUE (name),
    CONSTRAINT ck_agent_role CHECK (role IN ('IMPLEMENTER', 'REVIEWER', 'HUMAN'))
);

CREATE TABLE program
(
    id                  uuid                        NOT NULL,
    slug                varchar(100)                NOT NULL,
    name                varchar(200)                NOT NULL,
    description         varchar(2000),
    initial_prompt      text                        NOT NULL,
    status              varchar(20)                 NOT NULL,
    created_by_agent_id uuid                        NOT NULL,
    created_at          timestamp(6) with time zone NOT NULL,
    updated_at          timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_program PRIMARY KEY (id),
    CONSTRAINT uq_program_slug UNIQUE (slug),
    CONSTRAINT ck_program_status CHECK (status IN ('IDEA', 'ACTIVE', 'PAUSED', 'DONE', 'ABANDONED')),
    CONSTRAINT fk_program_created_by FOREIGN KEY (created_by_agent_id) REFERENCES agent (id)
);

CREATE TABLE milestone
(
    id                    uuid         NOT NULL,
    program_id            uuid         NOT NULL,
    title                 varchar(200) NOT NULL,
    description           varchar(2000),
    sort_order            integer      NOT NULL,
    completed_at          timestamp(6) with time zone,
    completed_by_agent_id uuid,
    CONSTRAINT pk_milestone PRIMARY KEY (id),
    CONSTRAINT fk_milestone_program FOREIGN KEY (program_id) REFERENCES program (id),
    CONSTRAINT fk_milestone_completed_by FOREIGN KEY (completed_by_agent_id) REFERENCES agent (id),
    CONSTRAINT ck_milestone_completion CHECK (
        (completed_at IS NULL AND completed_by_agent_id IS NULL)
            OR (completed_at IS NOT NULL AND completed_by_agent_id IS NOT NULL))
);

CREATE INDEX ix_milestone_program ON milestone (program_id, sort_order);

CREATE TABLE clarification
(
    id                   uuid                        NOT NULL,
    program_id           uuid                        NOT NULL,
    question             text                        NOT NULL,
    context              text,
    blocking             boolean                     NOT NULL,
    status               varchar(20)                 NOT NULL,
    asked_by_agent_id    uuid                        NOT NULL,
    asked_at             timestamp(6) with time zone NOT NULL,
    answer_text          text,
    chosen_option_id     uuid,
    answered_by_agent_id uuid,
    answered_at          timestamp(6) with time zone,
    CONSTRAINT pk_clarification PRIMARY KEY (id),
    CONSTRAINT ck_clarification_status CHECK (status IN ('OPEN', 'ANSWERED')),
    CONSTRAINT fk_clarification_program FOREIGN KEY (program_id) REFERENCES program (id),
    CONSTRAINT fk_clarification_asked_by FOREIGN KEY (asked_by_agent_id) REFERENCES agent (id),
    CONSTRAINT fk_clarification_answered_by FOREIGN KEY (answered_by_agent_id) REFERENCES agent (id),
    -- R14: an answer needs at least one of answerText and chosenOptionId, and an
    -- OPEN clarification carries no answer at all. Enforced here as well as in the
    -- service so a direct database edit cannot produce a half-answered row.
    CONSTRAINT ck_clarification_answer CHECK (
        (status = 'OPEN'
            AND answered_at IS NULL AND answered_by_agent_id IS NULL
            AND answer_text IS NULL AND chosen_option_id IS NULL)
            OR (status = 'ANSWERED'
            AND answered_at IS NOT NULL AND answered_by_agent_id IS NOT NULL
            AND (answer_text IS NOT NULL OR chosen_option_id IS NOT NULL)))
);

CREATE INDEX ix_clarification_program ON clarification (program_id, status);

CREATE TABLE clarification_option
(
    id               uuid         NOT NULL,
    clarification_id uuid         NOT NULL,
    sort_order       integer      NOT NULL,
    label            varchar(200) NOT NULL,
    rationale        text         NOT NULL,
    CONSTRAINT pk_clarification_option PRIMARY KEY (id),
    CONSTRAINT fk_option_clarification FOREIGN KEY (clarification_id) REFERENCES clarification (id)
);

CREATE INDEX ix_option_clarification ON clarification_option (clarification_id, sort_order);

-- clarification and clarification_option reference each other, so the back
-- reference is added once both tables exist.
ALTER TABLE clarification
    ADD CONSTRAINT fk_clarification_chosen_option
        FOREIGN KEY (chosen_option_id) REFERENCES clarification_option (id);

CREATE TABLE activity_entry
(
    id             uuid                        NOT NULL,
    program_id     uuid                        NOT NULL,
    entry_type     varchar(40)                 NOT NULL,
    actor_agent_id uuid,
    summary        text                        NOT NULL,
    occurred_at    timestamp(6) with time zone NOT NULL,
    CONSTRAINT pk_activity_entry PRIMARY KEY (id),
    CONSTRAINT fk_activity_program FOREIGN KEY (program_id) REFERENCES program (id),
    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_agent_id) REFERENCES agent (id)
);

CREATE INDEX ix_activity_program ON activity_entry (program_id, occurred_at DESC);

-- The only seed data in the project. This UUID is fixed and is handed to the
-- browser through PROGRAM_DASHBOARD_HUMAN_AGENT_ID so answers submitted from the
-- dashboard have an actor to be attributed to.
--
-- CURRENT_TIMESTAMP rather than now(): H2's now() is LOCALTIMESTAMP and carries
-- no time zone, which would make the seeded values depend on the server clock.
INSERT INTO agent (id, name, role, description, registered_at, last_seen_at)
VALUES (CAST('00000000-0000-0000-0000-000000000001' AS uuid), 'harald', 'HUMAN',
        'The human operator answering from the dashboard', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
