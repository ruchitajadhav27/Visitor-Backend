package com.visitor.Visitor_Backend.repository;

import com.visitor.Visitor_Backend.model.Enquiry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EnquiryRepository extends MongoRepository<Enquiry, String> {
}