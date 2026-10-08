package com.exportrace.service;

import com.exportrace.entity.Lot;
import com.exportrace.entity.LotStatus;
import com.exportrace.entity.SanitaryCertification;
import com.exportrace.repository.LotRepository;
import com.exportrace.repository.SanitaryCertificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CertificationService {

    @Autowired
    private SanitaryCertificationRepository certificationRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotStateMachineService stateMachineService;

    public List<SanitaryCertification> getAllCertifications() {
        return certificationRepository.findAll();
    }

    public SanitaryCertification getCertificationByLotId(Long lotId) {
        return certificationRepository.findByLoteId(lotId).orElse(null);
    }

    @Transactional
    public SanitaryCertification requestCertification(Long lotId, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        // P0 GATEWAY CHECK: Validate QA status and Cold Chain before initiating SANIPES request
        stateMachineService.validateCanStartCertification(lot);

        SanitaryCertification cert = certificationRepository.findByLoteId(lotId)
                .orElse(new SanitaryCertification());

        cert.setLote(lot);
        cert.setEstado("SOLICITADO");
        cert.setEntidadEmisora("SANIPES");
        cert.setFechaSolicitud(LocalDateTime.now());
        if (cert.getNumeroCertificado() == null || cert.getNumeroCertificado().isEmpty()) {
            cert.setNumeroCertificado("SANIPES-2026-" + String.format("%05d", (int)(Math.random() * 90000 + 10000)));
        }

        SanitaryCertification saved = certificationRepository.save(cert);

        // Execute state machine transition
        stateMachineService.transition(lot, LotStatus.IN_CERTIFICATION, userEmail, "LOGISTICA",
                "Solicitud de Certificación Sanitaria tramitada ante SANIPES. Expediente: " + cert.getNumeroCertificado());

        return saved;
    }

    @Transactional
    public SanitaryCertification approveCertification(Long lotId, String certNumber, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        SanitaryCertification cert = certificationRepository.findByLoteId(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe trámite de certificación iniciado para este lote"));

        // P0 GATEWAY CHECK
        stateMachineService.validateCanBeCertified(lot);

        cert.setEstado("APROBADO");
        cert.setNumeroCertificado(certNumber != null && !certNumber.trim().isEmpty() ? certNumber : "CS-2026-094182");
        cert.setFechaEmision(LocalDateTime.now());
        cert.setFechaVencimiento(LocalDateTime.now().plusMonths(6));
        cert.setPdfUrl("/documents/SANIPES_" + lot.getCodigo() + ".pdf");

        SanitaryCertification saved = certificationRepository.save(cert);

        // Execute transition to CERTIFIED
        stateMachineService.transition(lot, LotStatus.CERTIFIED, userEmail, "LOGISTICA",
                "Certificado Sanitario APROBADO por SANIPES: " + cert.getNumeroCertificado());

        return saved;
    }

    @Transactional
    public Lot enableForDispatch(Long lotId, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote no encontrado con ID: " + lotId));

        // P0 GATEWAY CHECK: Must be CERTIFIED with APROBADO sanitary cert
        stateMachineService.validateCanBeReadyForDispatch(lot);

        return stateMachineService.transition(lot, LotStatus.READY_FOR_DISPATCH, userEmail, "LOGISTICA",
                "Lote habilitado y transferido a cola de Despacho de Exportación");
    }
}
