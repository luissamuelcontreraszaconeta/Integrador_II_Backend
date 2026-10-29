package com.exportrace.entity.enums;

public enum IncidentPriority {
    P1_CRITICAL("P1 - Crítica", "Highest", 15, 120),
    P2_HIGH("P2 - Alta", "High", 30, 240),
    P3_MEDIUM("P3 - Media", "Medium", 120, 480),
    P4_LOW("P4 - Baja", "Low", 240, 1440);

    private final String label;
    private final String jiraPriorityName;
    private final int slaFirstResponseMinutes;
    private final int slaResolutionMinutes;

    IncidentPriority(String label, String jiraPriorityName, int slaFirstResponseMinutes, int slaResolutionMinutes) {
        this.label = label;
        this.jiraPriorityName = jiraPriorityName;
        this.slaFirstResponseMinutes = slaFirstResponseMinutes;
        this.slaResolutionMinutes = slaResolutionMinutes;
    }

    public String getLabel() {
        return label;
    }

    public String getJiraPriorityName() {
        return jiraPriorityName;
    }

    public int getSlaFirstResponseMinutes() {
        return slaFirstResponseMinutes;
    }

    public int getSlaResolutionMinutes() {
        return slaResolutionMinutes;
    }
}
