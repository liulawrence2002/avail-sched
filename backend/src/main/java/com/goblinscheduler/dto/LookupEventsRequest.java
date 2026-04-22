package com.goblinscheduler.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record LookupEventsRequest(
    @NotEmpty(message = "hostTokens is required")
    List<String> hostTokens
) {}
