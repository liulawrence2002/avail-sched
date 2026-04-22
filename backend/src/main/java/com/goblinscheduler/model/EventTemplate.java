package com.goblinscheduler.model;

import java.time.Instant;

public class EventTemplate {
    private Long id;
    private String name;
    private String description;
    private String timezone;
    private Integer slotMinutes;
    private Integer durationMinutes;
    private String dailyStartTime;
    private String dailyEndTime;
    private String resultsVisibility;
    private String location;
    private String meetingUrl;
    private Instant createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
    public Integer getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(Integer slotMinutes) { this.slotMinutes = slotMinutes; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public String getDailyStartTime() { return dailyStartTime; }
    public void setDailyStartTime(String dailyStartTime) { this.dailyStartTime = dailyStartTime; }
    public String getDailyEndTime() { return dailyEndTime; }
    public void setDailyEndTime(String dailyEndTime) { this.dailyEndTime = dailyEndTime; }
    public String getResultsVisibility() { return resultsVisibility; }
    public void setResultsVisibility(String resultsVisibility) { this.resultsVisibility = resultsVisibility; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
