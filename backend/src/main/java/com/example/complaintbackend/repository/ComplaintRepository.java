package com.example.complaintbackend.repository;

import com.example.complaintbackend.entity.Complaint;
import com.example.complaintbackend.entity.ComplaintStatus;
import com.example.complaintbackend.entity.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    Page<Complaint> findByCreatedById(Long userId, Pageable pageable);

    Page<Complaint> findByAssignedToId(Long agentId, Pageable pageable);

    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    Page<Complaint> findByPriority(Priority priority, Pageable pageable);

    Page<Complaint> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority) AND " +
           "(:categoryId IS NULL OR c.category.id = :categoryId) AND " +
           "(:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Complaint> findWithFilters(
            @Param("status") ComplaintStatus status,
            @Param("priority") Priority priority,
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("SELECT c FROM Complaint c WHERE c.createdBy.id = :userId AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:priority IS NULL OR c.priority = :priority)")
    Page<Complaint> findByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("status") ComplaintStatus status,
            @Param("priority") Priority priority,
            Pageable pageable
    );

    List<Complaint> findByIsDeletedFalseOrderByIdDesc();

    List<Complaint> findByIsDeletedTrueOrderByIdDesc();

    long countByIsDeletedFalse();

    long countByIsDeletedTrue();

    long countByStatus(ComplaintStatus status);

    long countByStatusAndIsDeletedFalse(ComplaintStatus status);

    long countByPriority(Priority priority);

    long countByPriorityAndIsDeletedFalse(Priority priority);

    @Query("SELECT c.status, COUNT(c) FROM Complaint c WHERE c.isDeleted = false GROUP BY c.status")
    List<Object[]> countGroupByStatus();

    @Query("SELECT c.priority, COUNT(c) FROM Complaint c WHERE c.isDeleted = false GROUP BY c.priority")
    List<Object[]> countGroupByPriority();
}
