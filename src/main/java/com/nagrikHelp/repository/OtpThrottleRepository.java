package com.nagrikHelp.repository;

import com.nagrikHelp.model.OtpThrottle;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface OtpThrottleRepository extends MongoRepository<OtpThrottle, String> {
    Optional<OtpThrottle> findByPhone(String phone);
}
