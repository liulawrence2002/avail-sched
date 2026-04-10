package com.goblin.scheduler.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * RFC 5545-aware writer for tiny VCALENDAR documents.
 *
 * <p>Replaces the inline string builder that used to live in {@code EventService.getIcs}. The
 * hardening Phase 1.3 calls for lives here:
 *
 * <ul>
 *   <li>Control characters (everything below {@code 0x20} and {@code 0x7F}) are stripped; TAB is
 *       the only exception. {@code CR}/{@code LF} are collapsed and re-emitted as the literal
 *       escape {@code \n} per §3.3.11.
 *   <li>{@code \}, {@code ,}, {@code ;}, and newlines are escaped in TEXT values.
 *   <li>Content lines are folded at 75 octets with {@code CRLF} + single space, counted in UTF-8
 *       bytes so multi-byte characters are never split across a fold boundary (§3.1).
 *   <li>Every line terminates with {@code CRLF}, including the final one (§3.1).
 * </ul>
 *
 * <p>The writer does no file I/O; it returns the calendar body as a string so the controller layer
 * owns the HTTP plumbing. Tests live in {@code IcsWriterTest} and the behavior-characterization
 * coverage lives in {@code IcsOutputCharacterizationTest} (both cover the adversarial fixtures from
 * Phase 0.4).
 */
public final class IcsWriter {

  /** Maximum octets per content line before folding. RFC 5545 §3.1. */
  static final int MAX_LINE_OCTETS = 75;

  /** Folding continuation prefix: CRLF + one whitespace (space). */
  private static final String FOLD_CONTINUATION = "\r\n ";

  private static final String CRLF = "\r\n";

  private static final DateTimeFormatter ICS_DATE =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
          .withLocale(Locale.US)
          .withZone(ZoneId.of("UTC"));

  private IcsWriter() {}

  /**
   * Build a single-VEVENT VCALENDAR for a finalized slot.
   *
   * @param uid unique identifier for the event (e.g. {@code publicId@goblin-scheduler}).
   * @param summary the event title.
   * @param description the event description, or a fallback if the caller has none.
   * @param dtStart the event start instant.
   * @param dtEnd the event end instant.
   * @param dtStamp the timestamp at which this ICS was generated.
   * @return the full VCALENDAR body as a string terminated by {@code CRLF}.
   */
  public static String writeVEvent(
      String uid,
      String summary,
      String description,
      Instant dtStart,
      Instant dtEnd,
      Instant dtStamp) {
    List<String> lines = new ArrayList<>(11);
    lines.add("BEGIN:VCALENDAR");
    lines.add("VERSION:2.0");
    lines.add("PRODID:-//Goblin Scheduler//EN");
    lines.add("BEGIN:VEVENT");
    lines.add(fold("UID:" + escapeText(uid)));
    lines.add("DTSTAMP:" + ICS_DATE.format(dtStamp));
    lines.add("DTSTART:" + ICS_DATE.format(dtStart));
    lines.add("DTEND:" + ICS_DATE.format(dtEnd));
    lines.add(fold("SUMMARY:" + escapeText(summary)));
    lines.add(fold("DESCRIPTION:" + escapeText(description)));
    lines.add("END:VEVENT");
    lines.add("END:VCALENDAR");
    // Join with CRLF and guarantee a trailing CRLF, matching RFC 5545 §3.1.
    return String.join(CRLF, lines) + CRLF;
  }

  /**
   * Escape a TEXT value per RFC 5545 §3.3.11 and strip disallowed control characters.
   *
   * <p>Order of operations: CRLF sequences collapse to a single LF first so that {@code \r\n} does
   * not produce two escape sequences; every remaining control character below 0x20 (except TAB) and
   * the DEL at 0x7F is dropped; newlines are emitted as literal {@code \n}; {@code \}, {@code ,},
   * and {@code ;} are backslash-escaped.
   */
  static String escapeText(String value) {
    if (value == null) {
      return "";
    }
    // Collapse CRLF/CR so each "line" in the source becomes a single LF. We escape that LF
    // below; the continuation pair \r\n must never survive into the output.
    String normalized = value.replace("\r\n", "\n").replace("\r", "\n");
    StringBuilder out = new StringBuilder(normalized.length() + 8);
    int i = 0;
    while (i < normalized.length()) {
      int cp = normalized.codePointAt(i);
      int step = Character.charCount(cp);
      if (cp == '\n') {
        out.append("\\n");
      } else if (cp == '\t') {
        out.append('\t');
      } else if (cp < 0x20 || cp == 0x7F) {
        // Drop other C0 controls and DEL silently.
      } else if (cp == '\\') {
        out.append("\\\\");
      } else if (cp == ',') {
        out.append("\\,");
      } else if (cp == ';') {
        out.append("\\;");
      } else {
        out.appendCodePoint(cp);
      }
      i += step;
    }
    return out.toString();
  }

  /**
   * Fold a single logical content line to at most {@value #MAX_LINE_OCTETS} UTF-8 octets per
   * physical line, emitting {@code CRLF} + space as the continuation marker. Handles multi-byte
   * characters by always cutting on code-point boundaries.
   */
  static String fold(String line) {
    byte[] allBytes = line.getBytes(StandardCharsets.UTF_8);
    if (allBytes.length <= MAX_LINE_OCTETS) {
      return line;
    }
    StringBuilder out = new StringBuilder(allBytes.length + allBytes.length / MAX_LINE_OCTETS * 3);
    int runningOctets = 0;
    int i = 0;
    while (i < line.length()) {
      int cp = line.codePointAt(i);
      int cpBytes = utf8ByteLength(cp);
      if (runningOctets + cpBytes > MAX_LINE_OCTETS) {
        out.append(FOLD_CONTINUATION);
        runningOctets = 1; // the leading space occupies one octet
      }
      out.appendCodePoint(cp);
      runningOctets += cpBytes;
      i += Character.charCount(cp);
    }
    return out.toString();
  }

  private static int utf8ByteLength(int codePoint) {
    if (codePoint < 0x80) {
      return 1;
    }
    if (codePoint < 0x800) {
      return 2;
    }
    if (codePoint < 0x10000) {
      return 3;
    }
    return 4;
  }
}
