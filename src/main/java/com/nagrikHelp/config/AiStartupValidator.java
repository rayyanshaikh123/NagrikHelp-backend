package com.nagrikHelp.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AiStartupValidator {

    @Value("${gemini.api.key:${GEMINI_API_KEY:}}")
    private String geminiKey;
    @Value("${tesseract.datapath:}")
    private String tessPath;
    @Value("${tesseract.lang:eng}")
    private String tessLang;
    @Value("${tesseract.nativeLibPath:}")
    private String nativeLibPath;

    @PostConstruct
    public void validate() {
        if (geminiKey == null || geminiKey.isBlank()) {
            log.warn("[AI-STARTUP] Gemini API key MISSING (gemini.api.key or GEMINI_API_KEY) - AI validation will fail.");
        } else {
            String masked = geminiKey.length() <= 8 ? "********" : geminiKey.substring(0,4) + "***" + geminiKey.substring(geminiKey.length()-4);
            log.info("[AI-STARTUP] Gemini key present (len={}, masked={})", geminiKey.length(), masked);
        }
        if (tessPath == null || tessPath.isBlank()) {
            log.info("[AI-STARTUP] tesseract.datapath not set (will rely on default installation path if available)");
        } else {
            log.info("[AI-STARTUP] Tesseract datapath configured: {} (lang={})", tessPath, tessLang);
        }
        if (nativeLibPath != null && !nativeLibPath.isBlank()) {
            // Set system property for JNA to locate libtesseract + leptonica
            System.setProperty("jna.library.path", nativeLibPath);
            log.info("[AI-STARTUP] Set jna.library.path to {}", nativeLibPath);
        }
    }
}
