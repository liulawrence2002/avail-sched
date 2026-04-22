package com.goblinscheduler.model;

import java.time.Instant;

public class Participant {
    private Long id;
    private Long eventId;
    private String token;
    private String displayName;
    private String email;
    private Instant createdAt;

    public Participant() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
