package com.example.complaintbackend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ComplaintBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(ComplaintBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initData(ComplaintRepository repo) {
        return args -> {
            repo.save(new Complaint("Electricity", "Street light not working", "Pending"));
            repo.save(new Complaint("Water", "Leak in pipeline", "In Progress"));
        };
    }
}
