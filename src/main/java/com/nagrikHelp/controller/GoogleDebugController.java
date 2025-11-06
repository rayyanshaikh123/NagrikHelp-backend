package com.nagrikHelp.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/google")
public class GoogleDebugController {

    @Value("${GOOGLE_CLIENT_ID:}")
    private String googleClientId;

    @Value("${google.enforce-aud:true}")
    private boolean enforceAud;

    @GetMapping("/config")
    public Map<String, Object> config() {
        Map<String, Object> m = new HashMap<>();
        m.put("googleClientId", googleClientId);
        m.put("googleEnforceAud", enforceAud);
        return m;
    }
}
