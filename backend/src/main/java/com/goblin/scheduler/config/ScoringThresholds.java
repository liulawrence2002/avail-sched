package com.goblin.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Availability-score thresholds used by {@code ScoringService} to bucket each participant into yes
 * / maybe / bribe / no. Kept as a {@code @ConfigurationProperties} record so thresholds can be
 * tuned via {@code app.scoring.*} without touching code, while still being validated on startup by
 * Spring Boot's constructor binding.
 *
 * <p>Defaults match the historical hand-tuned numbers (0.99 / 0.59 / 0.29) from the pre-split
 * {@code ScoringService} implementation and the behavior locked in by {@code ScoringServiceTest}.
 */
@ConfigurationProperties(prefix = "app.scoring")
public record ScoringThresholds(
    @DefaultValue("0.99") double yesThreshold,
    @DefaultValue("0.59") double maybeThreshold,
    @DefaultValue("0.29") double bribeThreshold) {}
