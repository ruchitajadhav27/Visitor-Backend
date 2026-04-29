package com.visitor.Visitor_Backend.repository;

import com.visitor.Visitor_Backend.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByAvailableTrue();
    List<Appointment> findByAvailableFalse();
    List<Appointment> findByEmail(String email);
}