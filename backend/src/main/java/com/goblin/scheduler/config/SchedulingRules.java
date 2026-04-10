package com.goblin.scheduler.config;

import java.util.Locale;
import java.util.Set;

/**
 * Domain constants that describe what the scheduler will accept.
 *
 * <p>Phase 2.1 of the launch-readiness plan extracted these from the magic-number soup inside
 * {@code EventService}. Keeping them in a single compact class makes it trivial to audit the
 * allowed set, and lets the V3 DB CHECK constraints stay in lock-step with the service layer.
 */
public final class SchedulingRules {

  /** Legal meeting durations in minutes. Mirrors the V3 {@code events_duration_minutes_chk}. */
  public static final Set<Integer> ALLOWED_DURATIONS = Set.of(30, 60, 90);

  /** Legal results-visibility values. Mirrors the V3 {@code events_results_visibility_chk}. */
  public static final Set<String> ALLOWED_VISIBILITIES = Set.of("aggregate_public", "host_only");

  /** Default visibility applied when the caller omits the field at create time. */
  public static final String DEFAULT_VISIBILITY = "aggregate_public";

  /** How many top-scoring slots the results endpoint returns. */
  public static final int TOP_SLOTS_COUNT = 5;

  private SchedulingRules() {}

  /**
   * Normalize a caller-supplied visibility: a blank or {@code null} value becomes {@link
   * #DEFAULT_VISIBILITY}, and a non-blank value is lower-cased with outer whitespace trimmed. The
   * result is not validated — use {@link #ALLOWED_VISIBILITIES#contains(Object)} for that.
   */
  public static String normalizeVisibility(String visibility) {
    if (visibility == null || visibility.isBlank()) {
      return DEFAULT_VISIBILITY;
    }
    return visibility.trim().toLowerCase(Locale.ROOT);
  }
}
