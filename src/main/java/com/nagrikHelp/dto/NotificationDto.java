package com.nagrikHelp.dto;

import com.nagrikHelp.model.UserNotification;
import lombok.Data;

@Data
public class NotificationDto {
    private String id;
    private String issueId;
    private String type;
    private String message;
    private long createdAt;
    private boolean read;

    public static NotificationDto from(UserNotification n) {
        NotificationDto d = new NotificationDto();
        d.setId(n.getId());
        d.setIssueId(n.getIssueId());
        d.setType(n.getType());
        d.setMessage(n.getMessage());
        d.setCreatedAt(n.getCreatedAt());
        d.setRead(n.isRead());
        return d;
    }
}
