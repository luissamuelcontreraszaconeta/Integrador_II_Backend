package com.exportrace.service;

import com.exportrace.entity.enums.IncidentImpact;
import com.exportrace.entity.enums.IncidentPriority;
import com.exportrace.entity.enums.IncidentUrgency;
import com.exportrace.entity.enums.WorkContinuity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
public class IncidentPriorityService {

    /**
     * Convierte la respuesta guiada "¿Puedes continuar con tu trabajo?" en IncidentUrgency.
     * - NO_PUEDE_CONTINUAR -> HIGH
     * - CONTINUA_PARCIALMENTE -> MEDIUM
     * - PUEDE_CONTINUAR -> LOW
     */
    public IncidentUrgency resolveUrgency(WorkContinuity workContinuity) {
        if (workContinuity == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta de continuidad operativa (workContinuity) es obligatoria.");
        }
        return switch (workContinuity) {
            case NO_PUEDE_CONTINUAR -> IncidentUrgency.HIGH;
            case CONTINUA_PARCIALMENTE -> IncidentUrgency.MEDIUM;
            case PUEDE_CONTINUAR -> IncidentUrgency.LOW;
        };
    }

    /**
     * Matriz centralizada y determinística Impacto × Urgencia -> Prioridad:
     * - HIGH + HIGH -> P1_CRITICAL
     * - HIGH + MEDIUM -> P2_HIGH
     * - HIGH + LOW -> P2_HIGH
     * - MEDIUM + HIGH -> P2_HIGH
     * - MEDIUM + MEDIUM -> P3_MEDIUM
     * - MEDIUM + LOW -> P3_MEDIUM
     * - LOW + HIGH -> P3_MEDIUM
     * - LOW + MEDIUM -> P4_LOW
     * - LOW + LOW -> P4_LOW
     */
    public IncidentPriority calculatePriority(IncidentImpact impact, IncidentUrgency urgency) {
        if (impact == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nivel de impacto es obligatorio para calcular la prioridad.");
        }
        if (urgency == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nivel de urgencia es obligatorio para calcular la prioridad.");
        }

        return switch (impact) {
            case HIGH -> switch (urgency) {
                case HIGH -> IncidentPriority.P1_CRITICAL;
                case MEDIUM, LOW -> IncidentPriority.P2_HIGH;
            };
            case MEDIUM -> switch (urgency) {
                case HIGH -> IncidentPriority.P2_HIGH;
                case MEDIUM, LOW -> IncidentPriority.P3_MEDIUM;
            };
            case LOW -> switch (urgency) {
                case HIGH -> IncidentPriority.P3_MEDIUM;
                case MEDIUM, LOW -> IncidentPriority.P4_LOW;
            };
        };
    }

    public LocalDateTime calculateFirstResponseTarget(LocalDateTime baseTime, IncidentPriority priority) {
        LocalDateTime start = baseTime != null ? baseTime : LocalDateTime.now();
        return start.plusMinutes(priority.getSlaFirstResponseMinutes());
    }

    public LocalDateTime calculateResolutionTarget(LocalDateTime baseTime, IncidentPriority priority) {
        LocalDateTime start = baseTime != null ? baseTime : LocalDateTime.now();
        return start.plusMinutes(priority.getSlaResolutionMinutes());
    }
}
