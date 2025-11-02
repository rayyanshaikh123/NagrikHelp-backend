package com.nagrikHelp.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

@Service
@Slf4j
public class OcrService {

    @Value("${tesseract.datapath:}")
    private String tessDataPath; // Path to parent directory containing tessdata folder

    @Value("${tesseract.lang:eng}")
    private String tessLang;

    @Value("${ai.ocr.autoDisableOnError:true}")
    private boolean autoDisableOnError;

    @Value("${ai.ocr.retryEachRequest:false}")
    private boolean retryEachRequest;

    private volatile boolean available = true;
    private volatile String lastError = null;

    public boolean isAvailable() { return available; }
    public String getLastError() { return lastError; }

    public synchronized void reload() {
        if (!available) {
            log.info("[OCR] Manual reload invoked; resetting availability state");
            available = true;
            lastError = null;
        }
    }

    public String extractText(String imageBase64) {
        if (!available && !retryEachRequest) {
            return ""; // previously disabled and not retrying each request
        }
        if (imageBase64 == null || imageBase64.isBlank()) return "";
        // Strip data URL prefix if present
        int commaIdx = imageBase64.indexOf(",");
        if (imageBase64.startsWith("data:") && commaIdx > 0) {
            imageBase64 = imageBase64.substring(commaIdx + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(imageBase64);
        } catch (IllegalArgumentException e) {
            log.warn("[OCR] Invalid base64 image: {}", e.getMessage());
            return "";
        }
        File temp = null;
        try {
            // Decode image via ImageIO to normalize and re-encode as PNG (lossless for OCR)
            try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes)) {
                BufferedImage buffered = ImageIO.read(bais);
                if (buffered == null) {
                    log.warn("[OCR] ImageIO could not decode image; writing raw bytes and attempting Tesseract anyway");
                    temp = Files.createTempFile("ocr-img-raw-", ".bin").toFile();
                    try (FileOutputStream fos = new FileOutputStream(temp)) { fos.write(bytes); }
                } else {
                    temp = Files.createTempFile("ocr-img-", ".png").toFile();
                    ImageIO.write(buffered, "png", temp);
                }
            }
            Tesseract t = new Tesseract();
            if (tessDataPath != null && !tessDataPath.isBlank()) {
                t.setDatapath(tessDataPath);
                // Defensive: verify traineddata exists to avoid native crash
                File trained = new File(tessDataPath + (tessDataPath.endsWith(File.separator) ? "" : File.separator) + "tessdata" + File.separator + tessLang + ".traineddata");
                if (!trained.exists()) {
                    log.error("[OCR] Missing traineddata file: {} (will skip OCR and return empty)", trained.getAbsolutePath());
                    lastError = "MISSING_TRAINEDDATA:" + trained.getAbsolutePath();
                    return "";
                }
            }
            t.setLanguage(tessLang);
            String raw = t.doOCR(temp);
            String cleaned = raw == null ? "" : raw.replaceAll("\\s+", " ").trim();
            log.debug("[OCR] Extracted text ({} chars)", cleaned.length());
            return cleaned;
        } catch (TesseractException te) {
            log.error("[OCR] Tesseract error: {}", te.getMessage());
            return "";
        } catch (IOException io) {
            log.error("[OCR] IO error: {}", io.getMessage());
            return "";
        } catch (UnsatisfiedLinkError | NoClassDefFoundError nativeErr) {
            lastError = nativeErr.getMessage();
            log.error("[OCR] Native library issue (tesseract/leptonica not found): {}", lastError);
            if (autoDisableOnError && !retryEachRequest) {
                available = false;
                log.warn("[OCR] Disabling OCR for remainder of runtime (autoDisableOnError=true, retryEachRequest=false)");
            }
            return ""; // upstream service will map this to specific error code
        } finally {
            if (temp != null && temp.exists()) {
                if (!temp.delete()) {
                    temp.deleteOnExit();
                }
            }
        }
    }
}
