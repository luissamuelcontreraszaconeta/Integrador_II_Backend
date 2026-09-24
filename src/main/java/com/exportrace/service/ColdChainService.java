package com.exportrace.service;

import com.exportrace.dto.ColdChainRecordDTO;
import com.exportrace.entity.ColdChainRecord;
import com.exportrace.entity.Lot;
import com.exportrace.repository.ColdChainRecordRepository;
import com.exportrace.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ColdChainService {

    @Autowired
    private ColdChainRecordRepository coldChainRepository;

    @Autowired
    private LotRepository lotRepository;

    public List<ColdChainRecordDTO> getLogsByLotId(Long lotId) {
        return coldChainRepository.findByLoteIdOrderByFechaHoraDesc(lotId).stream()
                .map(ColdChainRecordDTO::new)
                .toList();
    }

    @Transactional
    public ColdChainRecordDTO addTemperatureLog(Long lotId, ColdChainRecordDTO dto, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        ColdChainRecord record = new ColdChainRecord();
        record.setLote(lot);
        record.setFechaHora(LocalDateTime.now());
        record.setTemperaturaCelsius(dto.getTemperature() != null ? dto.getTemperature() : -18.0);
        record.setUbicacionCamara(dto.getLocation() != null ? dto.getLocation() : "Cámara de Almacenamiento #02");
        record.setResponsableNombre(dto.getResponsible() != null ? dto.getResponsible() : "Técnico Frigorífico QA");
        
        // Evaluate temperature status
        double temp = record.getTemperaturaCelsius();
        if (temp <= -18.0) {
            record.setEstadoMedicion("NORMAL");
        } else if (temp <= -12.0) {
            record.setEstadoMedicion("WARNING");
        } else {
            record.setEstadoMedicion("CRITICAL");
        }

        record.setObservaciones(dto.getObservations());

        ColdChainRecord saved = coldChainRepository.save(record);
        return new ColdChainRecordDTO(saved);
    }
}
