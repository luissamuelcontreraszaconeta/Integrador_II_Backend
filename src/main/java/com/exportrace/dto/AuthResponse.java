package com.exportrace.dto;

public class AuthResponse {
    private String token; // Access Token (short-lived JWT, 15m)
    private String refreshToken; // Refresh token (opaque token)
    private String sessionId;
    private SessionPolicyDTO sessionPolicy;
    private UserDTO user;

    public AuthResponse() {}

    public AuthResponse(String token, UserDTO user) {
        this.token = token;
        this.user = user;
    }

    public AuthResponse(String token, String refreshToken, String sessionId, SessionPolicyDTO sessionPolicy, UserDTO user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.sessionId = sessionId;
        this.sessionPolicy = sessionPolicy;
        this.user = user;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public SessionPolicyDTO getSessionPolicy() { return sessionPolicy; }
    public void setSessionPolicy(SessionPolicyDTO sessionPolicy) { this.sessionPolicy = sessionPolicy; }

    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
