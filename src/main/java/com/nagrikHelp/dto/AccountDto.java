package com.nagrikHelp.dto;

import com.nagrikHelp.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountDto {
    private String name;
    private String email;
    private String phone;
    private Boolean emailConsent;
    private Boolean emailVerified;
    private Boolean smsConsent;
    private Boolean phoneVerified;
    private Role role;
}
