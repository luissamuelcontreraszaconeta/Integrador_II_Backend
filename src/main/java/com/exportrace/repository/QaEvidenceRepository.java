package com.exportrace.repository;

import com.exportrace.entity.QaEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QaEvidenceRepository extends JpaRepository<QaEvidence, Long> {
    List<QaEvidence> findByInspectionIdAndActiveTrueOrderByUploadedAtDesc(Long inspectionId);
    List<QaEvidence> findByLotIdAndActiveTrueOrderByUploadedAtDesc(Long lotId);
    List<QaEvidence> findByInspectionIdOrderByUploadedAtDesc(Long inspectionId);
    List<QaEvidence> findByLotIdOrderByUploadedAtDesc(Long lotId);
    Optional<QaEvidence> findByIdAndActiveTrue(Long id);
    void deleteByInspectionId(Long inspectionId);
}
