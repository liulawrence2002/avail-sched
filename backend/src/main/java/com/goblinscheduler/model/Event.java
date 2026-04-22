package com.goblinscheduler.model;

import java.time.*;

public class Event {
    private Long id;
    private String publicId;
    private String hostToken;
    private String title;
    private String description;
    private String timezone;
    private int slotMinutes;
    private int durationMinutes;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime dailyStartTime;
    private LocalTime dailyEndTime;
    private String location;
    private String meetingUrl;
    private String resultsVisibility;
    private int viewCount;
    private int respondentCount;
    private Instant finalSlotStart;
    private Instant finalizedAt;
    private Instant createdAt;
    private Instant deletedAt;
    private Instant deadline;
    private boolean autoFinalize;
    private Instant reminderSentAt;

    public Event() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getHostToken() { return hostToken; }
    public void setHostToken(String hostToken) { this.hostToken = hostToken; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public int getSlotMinutes() { return slotMinutes; }
    public void setSlotMinutes(int slotMinutes) { this.slotMinutes = slotMinutes; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalTime getDailyStartTime() { return dailyStartTime; }
    public void setDailyStartTime(LocalTime dailyStartTime) { this.dailyStartTime = dailyStartTime; }

    public LocalTime getDailyEndTime() { return dailyEndTime; }
    public void setDailyEndTime(LocalTime dailyEndTime) { this.dailyEndTime = dailyEndTime; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getMeetingUrl() { return meetingUrl; }
    public void setMeetingUrl(String meetingUrl) { this.meetingUrl = meetingUrl; }

    public String getResultsVisibility() { return resultsVisibility; }
    public void setResultsVisibility(String resultsVisibility) { this.resultsVisibility = resultsVisibility; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getRespondentCount() { return respondentCount; }
    public void setRespondentCount(int respondentCount) { this.respondentCount = respondentCount; }

    public Instant getFinalSlotStart() { return finalSlotStart; }
    public void setFinalSlotStart(Instant finalSlotStart) { this.finalSlotStart = finalSlotStart; }

    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getDeletedAt() { return deletedAt; }
    public void setDeletedAt(Instant deletedAt) { this.deletedAt = deletedAt; }

    public boolean isFinalized() {
        return finalSlotStart != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }

    public boolean isAutoFinalize() { return autoFinalize; }
    public void setAutoFinalize(boolean autoFinalize) { this.autoFinalize = autoFinalize; }

    public Instant getReminderSentAt() { return reminderSentAt; }
    public void setReminderSentAt(Instant reminderSentAt) { this.reminderSentAt = reminderSentAt; }
}
