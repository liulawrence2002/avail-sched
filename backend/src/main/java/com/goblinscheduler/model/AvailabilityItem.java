package com.goblinscheduler.model;

import java.math.BigDecimal;
import java.time.Instant;

public class AvailabilityItem {
    private Long id;
    private Long participantId;
    private Instant slotStart;
    private BigDecimal weight;

    public AvailabilityItem() {}

    public AvailabilityItem(Long participantId, Instant slotStart, BigDecimal weight) {
        this.participantId = participantId;
        this.slotStart = slotStart;
        this.weight = weight;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getParticipantId() { return participantId; }
    public void setParticipantId(Long participantId) { this.participantId = participantId; }

    public Instant getSlotStart() { return slotStart; }
    public void setSlotStart(Instant slotStart) { this.slotStart = slotStart; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
}
