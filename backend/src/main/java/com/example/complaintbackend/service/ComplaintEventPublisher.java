package com.example.complaintbackend.service;

import com.example.complaintbackend.event.ComplaintAssignedEvent;
import com.example.complaintbackend.event.ComplaintCreatedEvent;
import com.example.complaintbackend.event.ComplaintStatusChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class ComplaintEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ComplaintEventPublisher.class);
    public static final String TOPIC_COMPLAINT_EVENTS = "complaint-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Optional injection allows application to run seamlessly even when Kafka is not configured
    public ComplaintEventPublisher(@Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishComplaintCreated(ComplaintCreatedEvent event) {
        log.info("Domain Event: ComplaintCreatedEvent for Complaint #{} ('{}')", event.getComplaintId(), event.getTitle());
        sendEvent(event.getComplaintId().toString(), event);
    }

    public void publishComplaintAssigned(ComplaintAssignedEvent event) {
        log.info("Domain Event: ComplaintAssignedEvent for Complaint #{} assigned to '{}'", event.getComplaintId(), event.getAssignedToName());
        sendEvent(event.getComplaintId().toString(), event);
    }

    public void publishComplaintStatusChanged(ComplaintStatusChangedEvent event) {
        log.info("Domain Event: ComplaintStatusChangedEvent for Complaint #{} [{} -> {}]", event.getComplaintId(), event.getPreviousStatus(), event.getNewStatus());
        sendEvent(event.getComplaintId().toString(), event);
    }

    private void sendEvent(String key, Object event) {
        if (kafkaTemplate != null) {
            try {
                kafkaTemplate.send(TOPIC_COMPLAINT_EVENTS, key, event);
                log.debug("Published event to Kafka topic '{}' with key '{}'", TOPIC_COMPLAINT_EVENTS, key);
            } catch (Exception e) {
                log.warn("Kafka broker unavailable; event logged locally without failing the business operation: {}", e.getMessage());
            }
        } else {
            log.debug("KafkaTemplate not active; event handled in-memory");
        }
    }
}
