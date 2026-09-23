package com.example.jwt.domain.module.exception;

/** Der module_service ist nicht erreichbar oder der Circuit Breaker ist offen -> HTTP 503. */
public class ModuleServiceUnavailableException extends RuntimeException {

  public ModuleServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
