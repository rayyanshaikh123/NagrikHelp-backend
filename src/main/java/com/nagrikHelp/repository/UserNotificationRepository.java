package com.nagrikHelp.repository;

import com.nagrikHelp.model.UserNotification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface UserNotificationRepository extends MongoRepository<UserNotification, String> {
    List<UserNotification> findTop50ByUserEmailOrderByCreatedAtDesc(String userEmail);
    long countByUserEmailAndReadIsFalse(String userEmail);
    List<UserNotification> findByUserEmailAndReadIsFalseOrderByCreatedAtDesc(String userEmail);
}
