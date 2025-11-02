package com.nagrikHelp.controller;

import com.nagrikHelp.dto.AIValidationImageRequest;
import com.nagrikHelp.dto.AIValidationResponse;
import com.nagrikHelp.service.AIValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AIValidationController {

    private final AIValidationService aiValidationService;

    @PostMapping("/validate")
    public ResponseEntity<AIValidationResponse> validate(@RequestBody AIValidationImageRequest request) {
        String img = request.getImageBase64();
        int len = img == null ? 0 : img.length();
        log.debug("[AI][CTRL] /validate received imageBase64 length={}", len);
        AIValidationResponse resp = aiValidationService.validateImage(img);
        return ResponseEntity.ok(resp);
    }
}
