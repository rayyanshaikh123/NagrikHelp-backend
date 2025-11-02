package com.nagrikHelp.dto;

import lombok.Data;

@Data
public class ShareSmsRequest {
    private String phone; // E.164
    private String message; // optional custom message
    private boolean subscribe; // if true add to followers
}
