package com.example.complaintbackend.config;

import com.example.complaintbackend.entity.*;
import com.example.complaintbackend.repository.CategoryRepository;
import com.example.complaintbackend.repository.ComplaintHistoryRepository;
import com.example.complaintbackend.repository.ComplaintRepository;
import com.example.complaintbackend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepo;
    private final CategoryRepository categoryRepo;
    private final ComplaintRepository complaintRepo;
    private final ComplaintHistoryRepository historyRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepo,
            CategoryRepository categoryRepo,
            ComplaintRepository complaintRepo,
            ComplaintHistoryRepository historyRepo,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepo = userRepo;
        this.categoryRepo = categoryRepo;
        this.complaintRepo = complaintRepo;
        this.historyRepo = historyRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking database seed records...");

        // 1. Seed Roles & Users
        User admin = userRepo.findByUsername("admin").orElseGet(() -> {
            log.info("Seeding default Admin user ('admin' / 'admin123')");
            return userRepo.save(new User(
                    "admin",
                    "admin@example.com",
                    passwordEncoder.encode("admin123"),
                    "Admin Administrator",
                    Role.ROLE_ADMIN
            ));
        });

        String[][] agentsData = {
            {"support_agent", "agent@example.com", "Sarah Jenkins"},
            {"agent_alex", "alex.rivera@example.com", "Alex Rivera"},
            {"agent_priya", "priya.sharma@example.com", "Priya Sharma"},
            {"agent_david", "david.chen@example.com", "David Chen"},
            {"agent_carlos", "carlos.mendez@example.com", "Carlos Mendez"},
            {"agent_emily", "emily.watson@example.com", "Emily Watson"},
            {"agent_michael", "michael.brown@example.com", "Michael Brown"},
            {"agent_fatima", "fatima.hassan@example.com", "Fatima Al-Hassan"},
            {"agent_liam", "liam.oconnor@example.com", "Liam O'Connor"},
            {"agent_aisha", "aisha.diallo@example.com", "Aisha Diallo"},
            {"agent_marcus", "marcus.vance@example.com", "Marcus Vance"},
            {"agent_ananya", "ananya.patel@example.com", "Ananya Patel"}
        };
        for (String[] data : agentsData) {
            String uName = data[0];
            String email = data[1];
            String fName = data[2];
            userRepo.findByUsername(uName).orElseGet(() -> {
                log.info("Seeding Support Agent ('{}' / 'agent123')", uName);
                return userRepo.save(new User(
                        uName, email, passwordEncoder.encode("agent123"), fName, Role.ROLE_SUPPORT_AGENT
                ));
            });
        }

        User agent = userRepo.findByUsername("support_agent").orElse(null);

        User demoUser = userRepo.findByUsername("demo_user").orElseGet(() -> {
            log.info("Seeding default Demo User ('demo_user' / 'user123')");
            return userRepo.save(new User(
                    "demo_user",
                    "user@example.com",
                    passwordEncoder.encode("user123"),
                    "Harini L",
                    Role.ROLE_USER
            ));
        });

        // 2. Seed Categories
        Category electricity = categoryRepo.findByNameIgnoreCase("Electricity").orElseGet(() ->
                categoryRepo.save(new Category("Electricity", "Power grid, street lighting, and wiring faults")));

        Category water = categoryRepo.findByNameIgnoreCase("Water").orElseGet(() ->
                categoryRepo.save(new Category("Water", "Pipeline leaks, drainage, and water supply issues")));

        categoryRepo.findByNameIgnoreCase("Sanitation").orElseGet(() ->
                categoryRepo.save(new Category("Sanitation", "Garbage collection and waste management")));

        categoryRepo.findByNameIgnoreCase("Roads & Infrastructure").orElseGet(() ->
                categoryRepo.save(new Category("Roads & Infrastructure", "Potholes, pavement, and traffic signals")));

        Category general = categoryRepo.findByNameIgnoreCase("General").orElseGet(() ->
                categoryRepo.save(new Category("General", "General queries and feedback")));

        // 3. Seed Initial Complaints if empty
        if (complaintRepo.count() == 0) {
            log.info("Seeding initial complaints...");

            Complaint c1 = new Complaint(
                    "Street light not working",
                    "Street light pole #42 outside Community Center is flickering and completely off at night.",
                    electricity,
                    Priority.HIGH,
                    ComplaintStatus.OPEN,
                    demoUser
            );
            Complaint saved1 = complaintRepo.save(c1);
            historyRepo.save(new ComplaintHistory(
                    saved1, "CREATED", null, ComplaintStatus.OPEN, demoUser, "Initial complaint logged"
            ));

            Complaint c2 = new Complaint(
                    "Leak in pipeline",
                    "Continuous water leaking from main pipeline junction near 2nd Cross road causing water stagnation.",
                    water,
                    Priority.CRITICAL,
                    ComplaintStatus.IN_PROGRESS,
                    demoUser
            );
            c2.setAssignedTo(agent);
            Complaint saved2 = complaintRepo.save(c2);
            historyRepo.save(new ComplaintHistory(
                    saved2, "CREATED", null, ComplaintStatus.OPEN, demoUser, "Initial complaint logged"
            ));
            historyRepo.save(new ComplaintHistory(
                    saved2, "ASSIGNED", ComplaintStatus.OPEN, ComplaintStatus.ASSIGNED, admin, "Assigned to Sarah Jenkins"
            ));
            historyRepo.save(new ComplaintHistory(
                    saved2, "STATUS_UPDATED", ComplaintStatus.ASSIGNED, ComplaintStatus.IN_PROGRESS, agent, "Maintenance team dispatched"
            ));

            log.info("Initial complaint seeding completed.");
        }
    }
}
