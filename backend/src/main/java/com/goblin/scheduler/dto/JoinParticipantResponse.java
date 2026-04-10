package com.goblin.scheduler.dto;

public record JoinParticipantResponse(
    String participantToken, String participantLink, boolean existingParticipant) {}
