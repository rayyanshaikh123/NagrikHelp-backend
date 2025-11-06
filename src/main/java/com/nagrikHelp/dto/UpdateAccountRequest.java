package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class UpdateAccountRequest {
    private String phone;
    private Boolean emailConsent;
    private Boolean smsConsent;
}
