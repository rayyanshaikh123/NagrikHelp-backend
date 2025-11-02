package com.nagrikHelp.dto;

import com.nagrikHelp.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Role role;
    private String name;
    private String email;
}
