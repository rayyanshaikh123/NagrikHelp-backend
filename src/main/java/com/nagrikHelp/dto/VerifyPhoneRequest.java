package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class VerifyPhoneRequest {
    private String phone;
    private String code;
}
