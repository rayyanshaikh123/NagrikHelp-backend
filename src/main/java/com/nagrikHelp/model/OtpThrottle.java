package com.nagrikHelp.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "otp_throttles")
public class OtpThrottle {
    @Id
    private String id;

    @Indexed(unique = true)
    private String phone;

    // epoch seconds of send events
    private List<Long> sends = new ArrayList<>();

    @Version
    private Long version;

    public void addSend(long epochSeconds) {
        sends.add(epochSeconds);
    }
}
