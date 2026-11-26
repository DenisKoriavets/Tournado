package com.tournado.core.support;

/**
 * The stable part of an RFC 7807 ProblemDetail (docs/contracts/error-format.md) that integration tests assert on:
 * HTTP status and the application error code. Localized {@code detail} text is intentionally not compared.
 */
public record ApiError(int status, String code) {
}
