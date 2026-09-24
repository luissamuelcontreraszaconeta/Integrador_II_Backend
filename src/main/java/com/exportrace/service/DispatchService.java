package com.exportrace.service;

import com.exportrace.entity.Dispatch;
import com.exportrace.entity.Lot;
import com.exportrace.entity.LotHistory;
import com.exportrace.repository.DispatchRepository;
import com.exportrace.repository.LotHistoryRepository;
import com.exportrace.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DispatchService {

    @Autowired
    private DispatchRepository dispatchRepository;

    @Autowired
    private LotRepository lotRepository;

    @Autowired
    private LotHistoryRepository lotHistoryRepository;

    public Dispatch getDispatchByLotId(Long lotId) {
        return dispatchRepository.findByLoteId(lotId).orElse(null);
    }

    @Transactional
    public Dispatch registerDispatch(Long lotId, Dispatch dispatchReq, String userEmail) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        Dispatch dispatch = dispatchRepository.findByLoteId(lotId)
                .orElse(new Dispatch());

        dispatch.setLote(lot);
        dispatch.setGuiaRemision(dispatchReq.getGuiaRemision() != null ? dispatchReq.getGuiaRemision() : "GR-2026-00451");
        dispatch.setDuasExportacion(dispatchReq.getDuasExportacion() != null ? dispatchReq.getDuasExportacion() : "118-2026-10-004521");
        dispatch.setPuertoOrigen("Puerto del Callao, Perú");
        dispatch.setPuertoDestino(dispatchReq.getPuertoDestino() != null ? dispatchReq.getPuertoDestino() : "Puerto de Qingdao, China");
        dispatch.setTransportista(dispatchReq.getTransportista() != null ? dispatchReq.getTransportista() : "Naviera Maersk Line / Transporte Terrestre TransMar S.A.C.");
        dispatch.setPlacaVehiculo(dispatchReq.getPlacaVehiculo() != null ? dispatchReq.getPlacaVehiculo() : "F3B-892");
        dispatch.setNumeroContenedor(dispatchReq.getNumeroContenedor() != null ? dispatchReq.getNumeroContenedor() : "MSKU-948192-0");
        dispatch.setPrecintoSeguridad(dispatchReq.getPrecintoSeguridad() != null ? dispatchReq.getPrecintoSeguridad() : "PS-SANIPES-88319");
        dispatch.setFechaDespacho(LocalDateTime.now());
        dispatch.setFechaEstimadaLlegada(LocalDateTime.now().plusDays(25));
        dispatch.setEstado("DESPACHADO");
        dispatch.setObservaciones(dispatchReq.getObservaciones());

        Dispatch saved = dispatchRepository.save(dispatch);

        // Update Lot Status
        String oldStatus = lot.getEstado();
        lot.setEstado("DISPATCHED");
        lot.setFechaActualizacion(LocalDateTime.now());
        lotRepository.save(lot);

        LotHistory history = new LotHistory(lot, oldStatus, "DISPATCHED", userEmail, "LOGISTICA", "Despacho de exportación ejecutado. Contenedor: " + saved.getNumeroContenedor());
        lotHistoryRepository.save(history);

        return saved;
    }
}
