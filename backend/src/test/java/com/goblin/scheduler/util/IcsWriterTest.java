package com.goblin.scheduler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class IcsWriterTest {

  private static final Instant DT_STAMP = Instant.parse("2026-03-15T00:00:00Z");
  private static final Instant DT_START = Instant.parse("2026-04-01T09:00:00Z");
  private static final Instant DT_END = Instant.parse("2026-04-01T10:00:00Z");

  private String render(String summary, String description) {
    return IcsWriter.writeVEvent(
        "pub123@goblin-scheduler", summary, description, DT_START, DT_END, DT_STAMP);
  }

  // ----- escapeText -----

  @Test
  void escapeText_commaSemicolonBackslashNewline_escaped() {
    assertEquals("a\\,b\\;c\\\\d\\ne", IcsWriter.escapeText("a,b;c\\d\ne"));
  }

  @Test
  void escapeText_normalizesCrlfToSingleLiteralEscape() {
    assertEquals("line one\\nline two", IcsWriter.escapeText("line one\r\nline two"));
    assertEquals("line one\\nline two", IcsWriter.escapeText("line one\rline two"));
  }

  @Test
  void escapeText_stripsOtherControlCharsButKeepsTab() {
    String input = "a\u0000b\u0007c\td\u001fe\u007Ff";
    assertEquals("abc\tdef", IcsWriter.escapeText(input));
  }

  @Test
  void escapeText_preservesUnicode() {
    assertEquals("Team ☕ weekly 🗓️", IcsWriter.escapeText("Team ☕ weekly 🗓️"));
  }

  @Test
  void escapeText_nullIsEmptyString() {
    assertEquals("", IcsWriter.escapeText(null));
  }

  // ----- fold -----

  @Test
  void fold_shortLine_unchanged() {
    String line = "SUMMARY:hello";
    assertEquals(line, IcsWriter.fold(line));
  }

  @Test
  void fold_exactly75Octets_unchanged() {
    String line = "x".repeat(75);
    assertEquals(line, IcsWriter.fold(line));
  }

  @Test
  void fold_76Octets_foldedOnceWithCrlfSpace() {
    String line = "x".repeat(76);
    String folded = IcsWriter.fold(line);
    assertEquals("x".repeat(75) + "\r\n x", folded);
  }

  @Test
  void fold_longSummary_everyPhysicalLineAtMost75Octets() {
    String line = "SUMMARY:" + "x".repeat(200);
    String folded = IcsWriter.fold(line);
    for (String physical : folded.split("\r\n", -1)) {
      int octets = physical.getBytes(StandardCharsets.UTF_8).length;
      assertTrue(
          octets <= IcsWriter.MAX_LINE_OCTETS,
          "Physical line '" + physical + "' is " + octets + " octets");
    }
  }

  @Test
  void fold_unfoldingRestoresOriginal() {
    String original = "SUMMARY:" + "x".repeat(200);
    String folded = IcsWriter.fold(original);
    String unfolded = folded.replace("\r\n ", "");
    assertEquals(original, unfolded);
  }

  @Test
  void fold_multiByteCharsAreNeverSplit() {
    // Build a line whose "natural" 75-octet boundary would land mid-character if we counted
    // characters instead of bytes: 24 emoji (each 4 bytes) followed by "x"*0 = 96 bytes.
    String line = "☕".repeat(30); // 3 bytes each = 90 bytes total
    String folded = IcsWriter.fold(line);
    String unfolded = folded.replace("\r\n ", "");
    assertEquals(line, unfolded);
    // The folded form must still decode as valid UTF-8 with the same code points.
    byte[] bytes = folded.getBytes(StandardCharsets.UTF_8);
    String roundTrip = new String(bytes, StandardCharsets.UTF_8);
    assertEquals(folded, roundTrip);
  }

  // ----- writeVEvent -----

  @Test
  void writeVEvent_emitsAllRequiredFieldsWithCrlf() {
    String ics = render("Team Sync", "Weekly catch-up");

    assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\n"));
    assertTrue(ics.contains("\r\nVERSION:2.0\r\n"));
    assertTrue(ics.contains("\r\nPRODID:-//Goblin Scheduler//EN\r\n"));
    assertTrue(ics.contains("\r\nBEGIN:VEVENT\r\n"));
    assertTrue(ics.contains("\r\nUID:pub123@goblin-scheduler\r\n"));
    assertTrue(ics.contains("\r\nDTSTAMP:20260315T000000Z\r\n"));
    assertTrue(ics.contains("\r\nDTSTART:20260401T090000Z\r\n"));
    assertTrue(ics.contains("\r\nDTEND:20260401T100000Z\r\n"));
    assertTrue(ics.contains("\r\nSUMMARY:Team Sync\r\n"));
    assertTrue(ics.contains("\r\nDESCRIPTION:Weekly catch-up\r\n"));
    assertTrue(ics.contains("\r\nEND:VEVENT\r\n"));
    assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
  }

  @Test
  void writeVEvent_adversarialTitle_commaSemicolonNewline_escaped() {
    String ics = render("a,b;c\nd", "desc");
    assertTrue(ics.contains("SUMMARY:a\\,b\\;c\\nd"));
  }

  @Test
  void writeVEvent_adversarialTitle_injectedBeginVevent_cannotSplitLine() {
    String ics = render("BEGIN:VEVENT\nSUMMARY:injected", "desc");
    assertTrue(ics.contains("SUMMARY:BEGIN:VEVENT\\nSUMMARY:injected"));
    long rogue = ics.lines().filter(line -> line.equals("SUMMARY:injected")).count();
    assertEquals(0, rogue);
  }

  @Test
  void writeVEvent_adversarialTitle_stripsBellCharacter() {
    String ics = render("Bell\u0007Title", "desc");
    assertTrue(ics.contains("SUMMARY:BellTitle"));
    assertFalse(ics.contains("\u0007"));
  }

  @Test
  void writeVEvent_longSummary_foldedWithCrlfSpace() {
    String longSummary = "x".repeat(200);
    String ics = render(longSummary, "desc");

    // Every physical line must fit within 75 octets.
    String[] physicalLines = ics.split("\r\n", -1);
    for (String line : physicalLines) {
      int octets = line.getBytes(StandardCharsets.UTF_8).length;
      assertTrue(octets <= IcsWriter.MAX_LINE_OCTETS, "Line '" + line + "' is " + octets);
    }
    // The unfolded form must contain the full "SUMMARY:xxx..." line.
    String unfolded = ics.replace("\r\n ", "");
    assertTrue(unfolded.contains("SUMMARY:" + longSummary));
  }

  @Test
  void writeVEvent_noDoubleCrlfBetweenLines() {
    String ics = render("Title", "Desc");
    assertFalse(ics.contains("\r\n\r\n"), "VCALENDAR body must not contain a blank line");
  }

  @Test
  void writeVEvent_linesJoinWithCrlfOnly() {
    String ics = render("Title", "Desc");
    // Split by CRLF and check no stray LF characters survive.
    String withoutCrlf = Arrays.stream(ics.split("\r\n", -1)).reduce("", String::concat);
    assertFalse(withoutCrlf.contains("\n"), "LF characters must only exist as part of CRLF pairs");
  }
}
