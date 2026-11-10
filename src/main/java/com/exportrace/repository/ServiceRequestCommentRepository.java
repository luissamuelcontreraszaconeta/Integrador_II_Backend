package com.exportrace.repository;

import com.exportrace.entity.ServiceRequestComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestCommentRepository extends JpaRepository<ServiceRequestComment, Long> {

    List<ServiceRequestComment> findByServiceRequestIdAndPublicCommentTrueOrderByCreatedAtAsc(Long serviceRequestId);

    Optional<ServiceRequestComment> findByJiraCommentId(String jiraCommentId);

    boolean existsByJiraCommentId(String jiraCommentId);
}
