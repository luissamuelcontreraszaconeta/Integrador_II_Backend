package com.exportrace.controller;

import com.exportrace.dto.LotDocumentDTO;
import com.exportrace.entity.Document;
import com.exportrace.entity.Lot;
import com.exportrace.repository.DocumentRepository;
import com.exportrace.repository.LotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LotRepository lotRepository;

    @GetMapping("/lot/{lotId}")
    public ResponseEntity<List<LotDocumentDTO>> getDocumentsByLotId(@PathVariable Long lotId) {
        List<Document> docs = documentRepository.findByLoteId(lotId);
        return ResponseEntity.ok(docs.stream().map(LotDocumentDTO::new).toList());
    }

    @PostMapping("/lot/{lotId}")
    public ResponseEntity<LotDocumentDTO> uploadDocument(
            @PathVariable Long lotId,
            @RequestBody Document docReq) {
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new RuntimeException("Lote no encontrado con ID: " + lotId));

        Document doc = new Document();
        doc.setLote(lot);
        doc.setNombre(docReq.getNombre() != null ? docReq.getNombre() : "Documento_Exportacion.pdf");
        doc.setTipo(docReq.getTipo() != null ? docReq.getTipo() : "CUSTOMS_DOC");
        doc.setUrl(docReq.getUrl() != null ? docReq.getUrl() : "/documents/" + doc.getNombre());
        doc.setSubidoPor(docReq.getSubidoPor() != null ? docReq.getSubidoPor() : "Sistema ExporTrace");
        doc.setFechaSubida(LocalDateTime.now());

        Document saved = documentRepository.save(doc);
        return ResponseEntity.ok(new LotDocumentDTO(saved));
    }
}
