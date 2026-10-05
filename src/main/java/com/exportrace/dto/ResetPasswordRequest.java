package com.exportrace.dto;

import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @Size(min = 6, message = "La contraseña temporal debe tener al menos 6 caracteres")
    private String newPassword;

    public ResetPasswordRequest() {}

    public ResetPasswordRequest(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
