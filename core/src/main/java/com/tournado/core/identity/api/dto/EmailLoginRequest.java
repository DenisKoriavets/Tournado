package com.tournado.core.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailLoginRequest(
        @NotBlank(message = "Email cannot be blank")
        @Email(message = "Email must be a valid email address")
        String email
) {
}
