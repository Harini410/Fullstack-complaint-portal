
package com.example.complaintbackend;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
@CrossOrigin(origins = "http://localhost:3000") 
public class ComplaintController {


    private final ComplaintRepository repository;

    public ComplaintController(ComplaintRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Complaint> getAllComplaints() {
        return repository.findAll();
    }

    @PostMapping
    public Complaint createComplaint(@RequestBody Complaint complaint) {
        return repository.save(complaint);
    }

    @PutMapping("/{id}")
    public Complaint updateComplaint(@PathVariable Long id, @RequestBody Complaint complaint) {
        return repository.findById(id)
                .map(c -> {
                    c.setCategory(complaint.getCategory());
                    c.setDescription(complaint.getDescription());
                    c.setStatus(complaint.getStatus());
                    return repository.save(c);
                }).orElseThrow(() -> new RuntimeException("Complaint not found"));
    }

    @DeleteMapping("/{id}")
    public void deleteComplaint(@PathVariable Long id) {
        repository.deleteById(id);
    }
}

