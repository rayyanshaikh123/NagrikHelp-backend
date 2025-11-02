package com.nagrikHelp.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.nagrikHelp.service.OcrService;

@Component
@Slf4j
public class GeminiVisionProvider implements VisionModelProvider {

    // Read from application.properties key `gemini.api.key` OR fallback to env var GEMINI_API_KEY
    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String apiKey;

    // Use text model endpoint now (OCR + text prompt)
    // Default updated to gemini-2.5-flash (v1beta). If this model 404s on v1beta, code retries with v1 automatically (HTTP fallback only).
    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String apiUrl;

    // Preferred model id when using official client
    @Value("${gemini.model:gemini-2.5-flash}")
    private String modelId;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private OcrService ocrService;

    @Value("${ai.ocr.enabled:true}")
    private boolean ocrEnabled;

    @Override
    public VisionResult analyze(String imageBase64) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            log.error("[AI] Gemini API key not configured. Provide via environment variable GEMINI_API_KEY or property gemini.api.key");
            throw new IllegalStateException("Gemini API key not configured");
        }
        String urlWithKey = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "key=" + apiKey;
        String ocrText = "";
        if (ocrEnabled) {
            if (ocrService.isAvailable()) {
                ocrText = ocrService.extractText(imageBase64);
                if (ocrText.isBlank()) {
                    log.debug("[AI] OCR produced no text; continuing with empty context");
                }
            } else {
                log.debug("[AI] OCR previously disabled after native error; skipping");
            }
        } else {
            log.debug("[AI] OCR disabled via ai.ocr.enabled=false");
        }
        String baseInstruction = "You classify civic infrastructure issues based on OCR text or short textual hints extracted from an image. " +
            "Return STRICT JSON ONLY with fields: issue (true|false), label (one of pothole, garbage, streetlight, water, other), confidence (0-1 number), reasoning (short).";
        String dynamicContext = ocrText.isBlank() ? "No OCR text available." : ("OCR text: " + ocrText);
        String fullPrompt = baseInstruction + "\n" + dynamicContext + "\nJSON:";

            // Raw HTTP path
        Map<String, Object> payload = new HashMap<>();
        List<Object> parts = new ArrayList<>();
        parts.add(Map.of("text", fullPrompt));
        payload.put("contents", List.of(Map.of("parts", parts)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);
        ResponseEntity<String> response = restTemplate.exchange(urlWithKey, HttpMethod.POST, entity, String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            String bodySnippet = response.getBody() == null ? "" : response.getBody().substring(0, Math.min(300, response.getBody().length()));
            if (response.getStatusCode() == HttpStatus.NOT_FOUND && apiUrl.contains("v1beta")) {
                // Attempt automatic fallback to v1 endpoint (replace v1beta with v1)
                String v1Url = apiUrl.replace("/v1beta/", "/v1/");
                if (!v1Url.equals(apiUrl)) {
                    String fallbackUrlWithKey = v1Url + (v1Url.contains("?") ? "&" : "?") + "key=" + apiKey;
                    log.warn("[AI] Model not found on v1beta. Retrying with v1 endpoint: {}", v1Url);
                    response = restTemplate.exchange(fallbackUrlWithKey, HttpMethod.POST, entity, String.class);
                }
            }
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("[AI] Gemini non-2xx status={} bodySnippet={}", response.getStatusCode(), bodySnippet);
                throw new IllegalStateException("Gemini API error: " + response.getStatusCode());
            }
        }
        String body = response.getBody();
        if (body == null || body.isBlank()) return new VisionResult(false, "unknown", 0.0, "Empty response from Gemini");

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(body);
            // Gemini response typically: { candidates: [ { content: { parts: [ { text: "..." } ] } } ] }
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
                // Try to locate JSON block
                int jsonStart = text.indexOf('{');
                int jsonEnd = text.lastIndexOf('}');
                if (jsonStart >= 0 && jsonEnd > jsonStart) {
                    String jsonFragment = text.substring(jsonStart, jsonEnd + 1);
                    JsonNode parsed = mapper.readTree(jsonFragment);
                    boolean issue = parsed.path("issue").asBoolean(false);
                    String label = parsed.path("label").asText("other");
                    double confidence = parsed.path("confidence").asDouble(0.4);
                    String reasoning = parsed.path("reasoning").asText("No reasoning provided");
                    return new VisionResult(issue, label, confidence, reasoning);
                }
                // Fallback heuristic parse: look for keywords if structured JSON missing
                String lower = text.toLowerCase(Locale.ROOT);
                boolean issue = lower.contains("pothole") || lower.contains("garbage") || lower.contains("light") || lower.contains("leak");
                String label = lower.contains("pothole") ? "pothole" : lower.contains("garbage") ? "garbage" : lower.contains("light") ? "streetlight" : lower.contains("leak") || lower.contains("water") ? "water" : "other";
                double confidence = issue ? 0.6 : 0.3;
                return new VisionResult(issue, label, confidence, text.length() > 160 ? text.substring(0,160) + "..." : text);
            }
            return new VisionResult(false, "unknown", 0.0, "[gemini] No candidates in response");
        } catch (Exception parseEx) {
            log.warn("Gemini parse error: {}", parseEx.getMessage());
            return new VisionResult(false, "unknown", 0.0, "[gemini-parse-error] " + parseEx.getMessage());
        }
    }

    @Override
    public String name() { return "gemini"; }

    private VisionResult parseGeminiText(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            int s = text.indexOf('{');
            int e = text.lastIndexOf('}');
            if (s >= 0 && e > s) {
                String json = text.substring(s, e+1);
                JsonNode node = mapper.readTree(json);
                boolean issue = node.path("issue").asBoolean(false);
                String label = node.path("label").asText("other");
                double confidence = node.path("confidence").asDouble(0.4);
                String reasoning = node.path("reasoning").asText("No reasoning provided");
                return new VisionResult(issue, label, confidence, reasoning);
            }
            String lower = text.toLowerCase(Locale.ROOT);
            boolean issue = lower.contains("pothole") || lower.contains("garbage") || lower.contains("light") || lower.contains("leak");
            String label = lower.contains("pothole") ? "pothole" : lower.contains("garbage") ? "garbage" : lower.contains("light") ? "streetlight" : lower.contains("leak") || lower.contains("water") ? "water" : "other";
            double confidence = issue ? 0.6 : 0.3;
            return new VisionResult(issue, label, confidence, text.length() > 160 ? text.substring(0,160) + "..." : text);
        } catch (Exception ignored) {
            return null;
        }
    }

    // no extra helper; Spring placeholder above handles both property and env var
}
