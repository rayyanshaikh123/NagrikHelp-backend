package com.nagrikHelp.dto;

import com.nagrikHelp.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreatedResponse {
    private String id;
    private String name;
    private String email;
    private Role role;
}
