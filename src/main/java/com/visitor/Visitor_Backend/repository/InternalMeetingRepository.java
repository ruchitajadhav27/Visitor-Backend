package com.visitor.Visitor_Backend.repository;

import com.visitor.Visitor_Backend.model.InternalMeeting; // Adjust this import to match your model path
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InternalMeetingRepository extends MongoRepository<InternalMeeting, String> {
    // You don't need to write any methods here. 
    // .findAll(), .save(), and .findById() are inherited automatically!
}