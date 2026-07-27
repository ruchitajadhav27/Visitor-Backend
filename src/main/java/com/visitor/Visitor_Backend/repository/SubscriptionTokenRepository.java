package com.visitor.Visitor_Backend.repository;

import com.visitor.Visitor_Backend.model.SubscriptionToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionTokenRepository extends MongoRepository<SubscriptionToken, String> {
	List<SubscriptionToken> findByEmail(String email);
}