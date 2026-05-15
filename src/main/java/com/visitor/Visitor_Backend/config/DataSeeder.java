package com.visitor.Visitor_Backend.config;

import com.visitor.Visitor_Backend.model.Admin;
import com.visitor.Visitor_Backend.repository.AdminRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;

    public DataSeeder(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Check if an admin already exists so we don't create duplicates
        if (adminRepository.count() == 0) {
            Admin admin = new Admin();
            admin.setUsername("admin"); // Set your permanent username
            admin.setPassword("admin"); // Set your permanent password
            
            adminRepository.save(admin);
            System.out.println("--- Admin account created permanently in MongoDB ---");
        } else {
            System.out.println("--- Admin account already exists in MongoDB ---");
        }
    }
}