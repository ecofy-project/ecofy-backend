package br.com.ecofy.ms_insights.core.domain;

import br.com.ecofy.ms_insights.core.domain.enums.GoalStatus;
import br.com.ecofy.ms_insights.core.domain.valueobject.Money;
import br.com.ecofy.ms_insights.core.domain.valueobject.UserId;

import java.time.Instant;
import java.util.UUID;

public class Goal {

    private final UUID id;
    private final UserId userId;
    private final String name;
    private final Money target;
    private final GoalStatus status;
    private final Instant createdAt;
    private final Instant updatedAt;

    public Goal(UUID id, UserId userId, String name, Money target, GoalStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.target = target;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UserId getUserId() { return userId; }
    public String getName() { return name; }
    public Money getTarget() { return target; }
    public GoalStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public Goal withUpdate(String name, Money target, GoalStatus status, Instant updatedAt) {
        return new Goal(this.id, this.userId, name, target, status, this.createdAt, updatedAt);
    }

}
