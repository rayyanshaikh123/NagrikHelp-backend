package com.nagrikHelp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;

    @Email
    @NotBlank
    private String email;

    private String phone;

    // Whether the user consents to receive email notifications
    private Boolean emailConsent = false;

    // Whether the user consents to receive SMS notifications
    private Boolean smsConsent = false;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;
}
