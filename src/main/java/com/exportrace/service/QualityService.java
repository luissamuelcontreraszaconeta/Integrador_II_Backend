package com.exportrace.service;

import com.exportrace.dto.QAInspectionDTO;
import com.exportrace.entity.Lot;
import com.exportrace.entity.LotHistory;
import com.exportrace.entity.QualityInspection;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.QualityInspectionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class QualityService {

    @Autowired
    private QualityInspectionRepository qualityRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    public QAInspectionDTO getInspectionByLotId(Long lotId) {
        QualityInspection qi = qualityRepository.findByLoteId(lotId)
                .orElse(null);
        return qi != null ? new QAInspectionDTO(qi) : null;
    }

    @Transactional
    public QAInspectionDTO saveInspection(Long lotId, QAInspectionDTO dto, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        QualityInspection qi = qualityRepository.findByLoteId(lotId)
                .orElse(new QualityInspection());

        qi.setLote(lot);
        qi.setFechaInspeccion(LocalDateTime.now());
        qi.setInspectorNombre(dto.getInspectorName() != null ? dto.getInspectorName() : "Dra. María Elena Quispe");
        qi.setApariencia(dto.getAppearance() != null ? dto.getAppearance() : "EXCELENTE");
        qi.setEvaluacionColor(dto.getColor() != null ? dto.getColor() : "CONFORME");
        qi.setTextura(dto.getTexture() != null ? dto.getTexture() : "FIRM");
        qi.setOlor(dto.getSmell() != null ? dto.getSmell() : "CARACTERISTICO");
        qi.setExamenParasitologico(dto.getParasiteCheck() != null ? dto.getParasiteCheck() : "AUSENCIA");
        qi.setResultadoOrganoleptico(dto.getOrganolepticResult() != null ? dto.getOrganolepticResult() : "CONFORME");
        qi.setObservaciones(dto.getObservations());

        if (dto.getEvidenceUrls() != null && !dto.getEvidenceUrls().isEmpty()) {
            qi.setEvidenciaFotosUrl(String.join(",", dto.getEvidenceUrls()));
        }

        QualityInspection saved = qualityRepository.save(qi);

        // Update lot state based on QA result
        String oldStatus = lot.getEstado();
        String newStatus = "CONFORME".equalsIgnoreCase(dto.getOrganolepticResult()) ? "READY_FOR_CERTIFICATION" : "OBSERVED";
        
        lot.setEstado(newStatus);
        lot.setFechaActualizacion(LocalDateTime.now());
        lotRepository.save(lot);

        // Record history
        LotHistory history = new LotHistory(lot, oldStatus, newStatus, qi.getInspectorNombre(), "QA", "Inspección de Calidad Registrada: " + dto.getOrganolepticResult());
        lotHistoryRepository.save(history);

        return new QAInspectionDTO(saved);
    }
}
