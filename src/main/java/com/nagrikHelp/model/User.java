package com.nagrikHelp.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "users")
public class User {
    @Id
    private String id;

    private String name;

    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true, sparse = true)
    private String phone;

    private String passwordHash;

    private Role role;
    // whether user consented to receive emails
    private Boolean emailConsent = false;
    // whether user consented to receive SMS
    private Boolean smsConsent = false;
    // whether user's phone has been verified via OTP
    private Boolean phoneVerified = false;
    // mail provider (inferred from email domain) e.g. gmail.com
    private String mailProvider;
}
