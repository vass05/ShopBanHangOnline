package com.helishop.core.security;

import java.security.Principal;

/** Identity taken exclusively from a verified access token. */
public record AuthenticatedUser(Long id, String email) implements Principal {
    @Override
    public String getName() {
        return email;
    }
}
