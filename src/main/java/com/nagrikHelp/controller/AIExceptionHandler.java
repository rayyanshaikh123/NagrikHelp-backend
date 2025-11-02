package com.nagrikHelp.controller;

import com.nagrikHelp.dto.AIValidationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ControllerAdvice
@Slf4j
public class AIExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    public AIValidationResponse handleAny(Exception ex) {
        // Only intercept AI related stack traces; still logs error.
        log.error("[AI][GLOBAL] Unhandled exception mapped to AIValidationResponse: {}", ex.getMessage());
        String msg = ex.getMessage() == null ? "AI internal error" : ex.getMessage();
        String lower = msg.toLowerCase();
        String code;
        if (lower.contains("api key not valid") || lower.contains("key invalid")) code = "AI_KEY_INVALID";
        else if (lower.contains("not configured")) code = "AI_KEY_MISSING";
        else if (lower.contains("no image")) code = "NO_IMAGE";
        else code = "AI_ERROR";
        return new AIValidationResponse(false, null, 0.0, msg, "gemini", true, code);
    }
}
