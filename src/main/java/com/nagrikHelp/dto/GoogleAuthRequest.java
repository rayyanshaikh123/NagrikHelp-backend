package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;
    private Boolean emailConsent = false;
    private Boolean smsConsent = false;
    private String phone;
}
