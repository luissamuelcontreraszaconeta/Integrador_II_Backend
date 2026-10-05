package com.exportrace.repository;

import com.exportrace.entity.QaEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QaEvidenceRepository extends JpaRepository<QaEvidence, Long> {
    List<QaEvidence> findByInspectionIdOrderByUploadedAtDesc(Long inspectionId);
    List<QaEvidence> findByLotIdOrderByUploadedAtDesc(Long lotId);
    void deleteByInspectionId(Long inspectionId);
}
