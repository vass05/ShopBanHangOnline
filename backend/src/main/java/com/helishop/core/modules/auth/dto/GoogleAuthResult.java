package com.helishop.core.modules.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GoogleAuthResult(String status, AuthResponse auth, String ticket, String email, String fullName) {
    public static GoogleAuthResult authenticated(AuthResponse auth) { return new GoogleAuthResult("AUTHENTICATED", auth, null, null, null); }
}
