package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class FollowRequest {
    private String phone;
    private String email;
    private String webhookUrl;
}
