package com.visitor.Visitor_Backend.repository;

import com.visitor.Visitor_Backend.model.Admin;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface AdminRepository extends MongoRepository<Admin, String> {
    Optional<Admin> findByUsername(String username); // Changed from findByEmail
}