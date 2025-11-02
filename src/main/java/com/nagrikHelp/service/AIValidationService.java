package com.nagrikHelp.service;

import com.nagrikHelp.dto.AIValidationResponse;
import com.nagrikHelp.model.IssueCategory;
import com.nagrikHelp.util.GeminiVisionProvider;
import com.nagrikHelp.util.VisionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIValidationService {

    private final GeminiVisionProvider geminiVisionProvider;
    @Value("${ai.enabled:true}")
    private boolean aiEnabled = true; // default for non-Spring tests

    @Value("${ai.heuristic.enabled:true}")
    private boolean heuristicEnabled = true; // default for non-Spring tests

    public AIValidationResponse validateImage(String imageBase64) {
        String correlationId = java.util.UUID.randomUUID().toString().substring(0,8);
        if (imageBase64 == null || imageBase64.isBlank()) {
            return new AIValidationResponse(false, null, 0.0, "No image provided", "none", true, "NO_IMAGE");
        }
        log.debug("[AI][{}] validateImage start size={} chars aiEnabled={} heuristicEnabled={}", correlationId, imageBase64.length(), aiEnabled, heuristicEnabled);
        if (!aiEnabled) {
            if (heuristicEnabled) {
                AIValidationResponse h = heuristic(imageBase64, correlationId, "AI_DISABLED");
                h.setError(false); // treat as success classification
                return h;
            }
            return new AIValidationResponse(false, null, 0.0, "AI disabled", "none", true, "AI_DISABLED");
        }
        VisionResult vr;
        try {
            vr = geminiVisionProvider.analyze(imageBase64);
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "AI failure" : ex.getMessage();
            String lower = msg.toLowerCase();
            String code;
            if (lower.contains("api key not valid") || lower.contains("key invalid") || lower.contains("unregistered callers") || lower.contains("permission_denied")) code = "AI_KEY_INVALID";
            else if (lower.contains("not configured")) code = "AI_KEY_MISSING";
            else if (lower.contains("unsatisfiedlinkerror") || lower.contains("native library") || lower.contains("libtesseract")) code = "OCR_NATIVE_MISSING";
            else if (lower.contains("model") && lower.contains("not found")) code = "AI_MODEL_NOT_FOUND";
            else code = "AI_ERROR";
            log.error("[AI][{}] Gemini/OCR error code={} message={}", correlationId, code, msg);
            if (heuristicEnabled) {
                AIValidationResponse h = heuristic(imageBase64, correlationId, code);
                // Mark error but still provide suggestion; frontend can choose to show fallback label.
                h.setError(true);
                h.setErrorCode(code);
                return h;
            }
            return new AIValidationResponse(false, null, 0.0, msg, "gemini", true, code);
        }
        if (vr == null) {
            log.error("[AI][{}] Null response from provider", correlationId);
            return new AIValidationResponse(false, null, 0.0, "Null response from AI provider", "gemini", true, "AI_NULL");
        }
        String mapped = mapLabelToCategory(vr.getLabel());
        log.debug("[AI][{}] Raw label='{}' mapped='{}' confidence={} issue={} reasoning='{}'", correlationId, vr.getLabel(), mapped, vr.getConfidence(), vr.isIssue(), vr.getReasoning());
        return new AIValidationResponse(vr.isIssue(), mapped, vr.getConfidence(), vr.getReasoning(), "gemini", false, null);
    }

    private String mapLabelToCategory(String label) {
        if (label == null) return null;
        String l = label.toLowerCase();
        if (l.contains("pothole")) return IssueCategory.POTHOLE.name();
        if (l.contains("garbage") || l.contains("trash") || l.contains("waste")) return IssueCategory.GARBAGE.name();
        if (l.contains("light") || l.contains("lamp")) return IssueCategory.STREETLIGHT.name();
        if (l.contains("water") || l.contains("leak") || l.contains("pipe")) return IssueCategory.WATER.name();
        return IssueCategory.OTHER.name();
    }

    private AIValidationResponse heuristic(String imageBase64, String cid, String reasonCode) {
        // Lightweight heuristic based on filename hint or base64 signature (no OCR text here)
        String lowerMeta = "";
        // Attempt to guess from small patterns (just for demo)
        if (imageBase64.length() < 120 && imageBase64.contains("/")) {
            lowerMeta = imageBase64.toLowerCase();
        }
        // Basic category extraction from pseudo filename or tokens
        String[] tokens = {"pothole","garbage","trash","waste","light","lamp","water","leak","pipe"};
        String matched = null;
        for (String t : tokens) {
            if (lowerMeta.contains(t)) { matched = t; break; }
        }
        String category;
        if (matched == null) category = IssueCategory.OTHER.name();
        else category = mapLabelToCategory(matched);
        double confidence = matched == null ? 0.25 : 0.5;
        String msg = matched == null ? "Heuristic fallback: could not infer from image metadata" : "Heuristic fallback matched token '"+matched+"'";
        log.warn("[AI][{}] Heuristic fallback used (reason={}) category={}", cid, reasonCode, category);
        return new AIValidationResponse(true, category, confidence, msg, "heuristic", false, null);
    }
}
