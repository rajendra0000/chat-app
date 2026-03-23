package com.chatapp.backend;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.chatapp.backend.ai.AiConfig;
import com.chatapp.backend.ai.AiService;

/**
 * Tests for @kalori mention detection and stripping.
 */
class AiMentionDetectionTest {

    private AiService aiService;

    @BeforeEach
    void setUp() throws Exception {
        // Create a minimal AiService with just AiConfig for mention detection
        // Using reflection to construct since we only need mention logic
        AiConfig config = new AiConfig();
        // Set bot-name via reflection since @Value won't work in unit test
        var botNameField = AiConfig.class.getDeclaredField("botName");
        botNameField.setAccessible(true);
        botNameField.set(config, "kalori");

        aiService = new AiService(null, config, null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Detects @kalori at start of message")
    void detectsMentionAtStart() {
        assertTrue(aiService.containsKaloriMention("@kalori explain Redis"));
    }

    @Test
    @DisplayName("Detects @kalori in middle of message")
    void detectsMentionInMiddle() {
        assertTrue(aiService.containsKaloriMention("Hey @kalori what is Java?"));
    }

    @Test
    @DisplayName("Detects @kalori at end of message")
    void detectsMentionAtEnd() {
        assertTrue(aiService.containsKaloriMention("explain this @kalori"));
    }

    @Test
    @DisplayName("Detects @Kalori case-insensitive")
    void detectsCaseInsensitive() {
        assertTrue(aiService.containsKaloriMention("@KALORI help me"));
        assertTrue(aiService.containsKaloriMention("@Kalori help me"));
    }

    @Test
    @DisplayName("Returns false for no mention")
    void noMention() {
        assertFalse(aiService.containsKaloriMention("Hello there"));
        assertFalse(aiService.containsKaloriMention("kalori without at sign"));
    }

    @Test
    @DisplayName("Returns false for null message")
    void nullMessage() {
        assertFalse(aiService.containsKaloriMention(null));
    }

    @Test
    @DisplayName("Strips @kalori from message")
    void stripsMention() {
        assertEquals("explain Redis", aiService.stripMention("@kalori explain Redis"));
        assertEquals("what is Java?", aiService.stripMention("Hey @kalori what is Java?").replaceFirst("^Hey\\s+", "").trim());
    }

    @Test
    @DisplayName("Strip returns empty for null")
    void stripNull() {
        assertEquals("", aiService.stripMention(null));
    }
}
