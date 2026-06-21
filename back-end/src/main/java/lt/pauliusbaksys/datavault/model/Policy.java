package lt.pauliusbaksys.datavault.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lt.pauliusbaksys.datavault.enums.PolicyStatus;
import lt.pauliusbaksys.datavault.enums.PolicyType;
import lt.pauliusbaksys.datavault.enums.WarningChannel;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "conditional_release_policies")
public class Policy {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "owner_user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User owner;

    @Column(name = "policy_name", nullable = false)
    private String policyName;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private PolicyType type;

    @Column(name = "inactivity_days")
    @Min(1)
    private Integer inactivityDays;

    @Column(name = "grace_days")
    @Min(1)
    private Integer graceDays;

    @Column(name = "scheduled_release_at")
    private OffsetDateTime scheduledReleaseAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "warning_channel", length = 32, nullable = false)
    private WarningChannel warningChannel = WarningChannel.TELEGRAM;

    @Column(name = "warning_every_hours")
    @Min(1)
    private Integer warningEveryHours;

    @Column(name = "warn_before_days")
    @Min(1)
    private Integer warnBeforeDays;

    @Column(name = "last_heartbeat_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastHeartbeatAt;

    @Column(name = "grace_started_at", columnDefinition = "timestamptz")
    private OffsetDateTime graceStartedAt;

    @Column(name = "last_warning_sent_at", columnDefinition = "timestamptz")
    private OffsetDateTime lastWarningSentAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 32, nullable = false)
    private PolicyStatus status = PolicyStatus.ACTIVE;

    @Column(name = "released_at", columnDefinition = "timestamptz")
    private OffsetDateTime releasedAt = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Policy() {
    }

    public Policy(UUID id, User owner, String policyName, PolicyType type, Integer inactivityDays, Integer graceDays, OffsetDateTime scheduledReleaseAt, Integer warningEveryHours, Integer warnBeforeDays, OffsetDateTime createdAt) {
        this.id = id;
        this.owner = owner;
        this.policyName = policyName;
        this.type = type;
        this.inactivityDays = inactivityDays;
        this.graceDays = graceDays;
        this.scheduledReleaseAt = scheduledReleaseAt;
        this.warningEveryHours = warningEveryHours;
        this.warnBeforeDays = warnBeforeDays;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getPolicyName() {
        return policyName;
    }

    public PolicyType getType() {
        return type;
    }

    public Integer getInactivityDays() {
        return inactivityDays;
    }

    public Integer getGraceDays() {
        return graceDays;
    }

    public OffsetDateTime getScheduledReleaseAt() {
        return scheduledReleaseAt;
    }

    public WarningChannel getWarningChannel() {
        return warningChannel;
    }

    public Integer getWarningEveryHours() {
        return warningEveryHours;
    }

    public Integer getWarnBeforeDays() {
        return warnBeforeDays;
    }

    public OffsetDateTime getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public OffsetDateTime getGraceStartedAt() {
        return graceStartedAt;
    }

    public OffsetDateTime getLastWarningSentAt() {
        return lastWarningSentAt;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public OffsetDateTime getReleasedAt() {
        return releasedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }


    public void setId(UUID id) {
        this.id = id;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public void setType(PolicyType type) {
        this.type = type;
    }

    public void setInactivityDays(Integer inactivityDays) {
        this.inactivityDays = inactivityDays;
    }

    public void setGraceDays(Integer graceDays) {
        this.graceDays = graceDays;
    }

    public void setScheduledReleaseAt(OffsetDateTime scheduledReleaseAt) {
        this.scheduledReleaseAt = scheduledReleaseAt;
    }

    public void setWarningChannel(WarningChannel warningChannel) {
        this.warningChannel = warningChannel;
    }

    public void setWarningEveryHours(Integer warningEveryHours) {
        this.warningEveryHours = warningEveryHours;
    }

    public void setWarnBeforeDays(Integer warnBeforeDays) {
        this.warnBeforeDays = warnBeforeDays;
    }

    public void setLastHeartbeatAt(OffsetDateTime lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public void setGraceStartedAt(OffsetDateTime graceStartedAt) {
        this.graceStartedAt = graceStartedAt;
    }

    public void setLastWarningSentAt(OffsetDateTime lastWarningSentAt) {
        this.lastWarningSentAt = lastWarningSentAt;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public void setReleasedAt(OffsetDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
