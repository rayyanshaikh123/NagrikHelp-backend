package com.nagrikHelp.service;

import com.nagrikHelp.dto.AIValidationResponse;
import com.nagrikHelp.util.GeminiVisionProvider;
import com.nagrikHelp.util.VisionResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AIValidationServiceTest {

    static class StubGeminiProvider extends GeminiVisionProvider {
        @Override
        public VisionResult analyze(String imageBase64) {
            return new VisionResult(true, "pothole", 0.93, "Stub detection pothole");
        }
    }

    @Test
    void validateImage_mapsLabelAndConfidence() {
        AIValidationService svc = new AIValidationService(new StubGeminiProvider());
        AIValidationResponse resp = svc.validateImage("abc123");
        assertTrue(resp.isValid());
        assertEquals("POTHOLE", resp.getSuggestedCategory());
        assertEquals("gemini", resp.getProvider());
        assertTrue(resp.getConfidence() > 0.9);
    }
}
