package com.goblinscheduler.dto;

public record JoinParticipantResponse(
    String participantToken,
    String participantLink,
    boolean existingParticipant
) {}
