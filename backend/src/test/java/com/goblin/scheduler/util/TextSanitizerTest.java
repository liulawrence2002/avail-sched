package com.goblin.scheduler.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextSanitizerTest {

    @Test
    void plainTextPassesThrough() {
        assertEquals("Hello World", TextSanitizer.sanitize("Hello World"));
    }

    @Test
    void stripsHtmlTags() {
        assertEquals("Hello World", TextSanitizer.sanitize("<b>Hello</b> <i>World</i>"));
    }

    @Test
    void stripsScriptTags() {
        assertEquals("alert('xss')", TextSanitizer.sanitize("<script>alert('xss')</script>"));
    }

    @Test
    void stripsNestedHtml() {
        assertEquals("text", TextSanitizer.sanitize("<div><span>text</span></div>"));
    }

    @Test
    void removesControlChars() {
        assertEquals("AB", TextSanitizer.sanitize("A\u0000B"));
        assertEquals("AB", TextSanitizer.sanitize("A\u0007B"));
    }

    @Test
    void preservesTabNewlineCr() {
        assertEquals("A\tB\nC\rD", TextSanitizer.sanitize("A\tB\nC\rD"));
    }

    @Test
    void nullReturnsNull() {
        assertNull(TextSanitizer.sanitize(null));
    }

    @Test
    void trimsWhitespace() {
        assertEquals("hello", TextSanitizer.sanitize("  hello  "));
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertEquals("", TextSanitizer.sanitize(""));
    }

    @Test
    void combinedHtmlAndControlChars() {
        assertEquals("clean", TextSanitizer.sanitize("<b>clean\u0000</b>"));
    }
}
