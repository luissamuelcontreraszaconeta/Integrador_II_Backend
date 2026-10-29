package com.exportrace.repository;

import com.exportrace.entity.IncidentReport;
import com.exportrace.entity.enums.IncidentPriority;
import com.exportrace.entity.enums.IncidentStatus;
import com.exportrace.entity.enums.JiraSyncStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    Optional<IncidentReport> findByCode(String code);

    Optional<IncidentReport> findByCorrelationId(String correlationId);

    Optional<IncidentReport> findByJiraIssueKey(String jiraIssueKey);

    boolean existsByCode(String code);

    boolean existsByCorrelationId(String correlationId);

    List<IncidentReport> findByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);

    List<IncidentReport> findAllByOrderByCreatedAtDesc();

    List<IncidentReport> findByStatusOrderByCreatedAtDesc(IncidentStatus status);

    List<IncidentReport> findByPriorityOrderByCreatedAtDesc(IncidentPriority priority);

    List<IncidentReport> findBySyncStatusOrderByCreatedAtAsc(JiraSyncStatus syncStatus);
}
