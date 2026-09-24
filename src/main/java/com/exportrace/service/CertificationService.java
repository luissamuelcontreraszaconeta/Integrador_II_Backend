package com.exportrace.service;

import com.exportrace.entity.Lot;
import com.exportrace.entity.LotHistory;
import com.exportrace.entity.SanitaryCertification;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.SanitaryCertificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CertificationService {

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    public List<SanitaryCertification> getAllCertifications() {
        return certificationRepository.findAll();
    }

    public SanitaryCertification getCertificationByLotId(Long lotId) {
        return certificationRepository.findByLoteId(lotId).orElse(null);
    }

    @Transactional
    public SanitaryCertification requestCertification(Long lotId, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        SanitaryCertification cert = certificationRepository.findByLoteId(lotId)
                .orElse(new SanitaryCertification());

        cert.setLote(lot);
        cert.setEstado("SOLICITADO");
        cert.setEntidadEmisora("SANIPES");
        cert.setFechaSolicitud(LocalDateTime.now());
        cert.setNumeroCertificado("SANIPES-2026-" + String.format("%05d", (int)(Math.random() * 90000 + 10000)));

        SanitaryCertification saved = certificationRepository.save(cert);

        // Update lot state
        String oldStatus = lot.getEstado();
        lot.setEstado("IN_CERTIFICATION");
        lot.setFechaActualizacion(LocalDateTime.now());
        lotRepository.save(lot);

        LotHistory history = new LotHistory(lot, oldStatus, "IN_CERTIFICATION", userEmail, "LOGISTICA", "Solicitud de Certificación Sanitaria tramitada ante SANIPES");
        lotHistoryRepository.save(history);

        return saved;
    }

    @Transactional
    public SanitaryCertification approveCertification(Long lotId, String certNumber, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        SanitaryCertification cert = certificationRepository.findByLoteId(lotId)
                .orElseThrow(() -> new RuntimeException("No existe trámite de certificación para este lote"));

        cert.setEstado("APROBADO");
        cert.setNumeroCertificado(certNumber != null ? certNumber : "CS-2026-094182");
        cert.setFechaEmision(LocalDateTime.now());
        cert.setFechaVencimiento(LocalDateTime.now().plusMonths(6));
        cert.setPdfUrl("/documents/SANIPES_" + lot.getCodigo() + ".pdf");

        SanitaryCertification saved = certificationRepository.save(cert);

        // Update lot state
        String oldStatus = lot.getEstado();
        lot.setEstado("CERTIFIED");
        lot.setFechaActualizacion(LocalDateTime.now());
        lotRepository.save(lot);

        LotHistory history = new LotHistory(lot, oldStatus, "CERTIFIED", userEmail, "LOGISTICA", "Certificado Sanitario APROBADO por SANIPES: " + cert.getNumeroCertificado());
        lotHistoryRepository.save(history);

        return saved;
    }
}
