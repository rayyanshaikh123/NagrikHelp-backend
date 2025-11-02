package com.nagrikHelp.controller;

import com.nagrikHelp.service.OcrService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/ocr")
@RequiredArgsConstructor
public class AIAdminController {

    private final OcrService ocrService;

    @GetMapping("/health")
    public ResponseEntity<Map<String,Object>> health() {
        return ResponseEntity.ok(Map.of(
                "available", ocrService.isAvailable(),
                "lastError", ocrService.getLastError(),
                "retryEachRequest", Boolean.getBoolean("ai.ocr.retryEachRequest")
        ));
    }

    @PostMapping("/reload")
    public ResponseEntity<Map<String,Object>> reload() {
        ocrService.reload();
        return ResponseEntity.ok(Map.of(
                "available", ocrService.isAvailable(),
                "lastError", ocrService.getLastError()
        ));
    }
}
